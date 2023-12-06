package com.untitled.ecm;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.untitled.ecm.constants.DakiyaStrings;
import com.untitled.ecm.core.DakiyaBackGroundTasksManager;
import com.untitled.ecm.core.DakiyaRuntimeSettings;
import com.untitled.ecm.core.DakiyaUserAccessLogger;
import com.untitled.ecm.dao.*;
import com.untitled.ecm.dao.external.RedshiftDao;
import com.untitled.ecm.dao.mappers.IllegalArgumentExceptionMapper;
import com.untitled.ecm.exceptionmappers.*;
import com.untitled.ecm.filter.RBACFilter;
import com.untitled.ecm.health.BackGroundTaskManagerHealthCheck;
import com.untitled.ecm.health.SchedulerHealthCheck;
import com.untitled.ecm.resources.*;
import com.untitled.ecm.services.scheduler.SchedulerManager;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.logging.PrintStreamLog;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.EnumSet;

@Slf4j
public class DakiyaApplication extends Application<DakiyaConfiguration> {
    private DakiyaConfiguration dakiyaConfiguration;
    private Environment environment;
    private JerseyEnvironment jerseyEnvironment;
    private DakiyaRuntimeSettings dakiyaRuntimeSettings;

    private CampaignDAO campaignDAO;
    private DakiyaUserDAO dakiyaUserDAO;
    private DakiyaUserDetailsDAO dakiyaUserDetailsDAO;
    private DakDAO dakDAO;
    private LogDAO logDAO;
    private RedshiftDao redshiftDao;

    private SchedulerManager schedulerManager;

    public static void main(final String[] args) throws Exception {
        new DakiyaApplication().run(args);
    }

    @Override
    public String getName() {
        return DakiyaStrings.DAKIYA_NAME;
    }

    @Override
    public void initialize(final Bootstrap<DakiyaConfiguration> bootstrap) {
        bootstrap.addBundle(new MigrationsBundle<DakiyaConfiguration>() {
            @Override
            public DataSourceFactory getDataSourceFactory(DakiyaConfiguration configuration) {
                return configuration.getDakiyaDataSourceFactory();
            }

            @Override
            public String name() {
                return "dakiya-db";
            }

            @Override
            public String getMigrationsFileName() {
                return "migrations/dakiya-migrations.xml";
            }
        });
        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );
    }

    @Override
    public void run(final DakiyaConfiguration configuration, final Environment dwEnvironment) {
        environment = dwEnvironment;
        jerseyEnvironment = dwEnvironment.jersey();
        dakiyaConfiguration = configuration;

        ObjectMapper objectMapper = environment.getObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        if (!(this.initializeDBIs() && this.initializeDAOs())) {
            log.error("dakiya initialization failed, check db");
            return;
        }

        if (this.initializeDakiyaRuntimeSettings()) {
            jerseyEnvironment.register(new SendGridResource(dakiyaRuntimeSettings));
            jerseyEnvironment.register(new DakiyaSettingResource(dakiyaRuntimeSettings));
            jerseyEnvironment.register(new DakResource(dakDAO, dakiyaRuntimeSettings));
            jerseyEnvironment.register(new SendgridWebhookResource(logDAO));
            if (!dakiyaRuntimeSettings.isSendingEmailAllowed()) {
                log.debug("Initialization: no emails will be sent unless settings is changed");
            }

        }

        if (this.initializeScheduler()) {
            jerseyEnvironment.register(new SchedulerResource(campaignDAO, dakDAO, dakiyaRuntimeSettings,
                    configuration.getInstanceType()));
            jerseyEnvironment.register(new CampaignResource(campaignDAO, dakDAO, redshiftDao,
                    dakiyaRuntimeSettings, schedulerManager, configuration.getInstanceType()));
        }

        this.startBackGroundTasksManager();

        this.setupAuthentication();

        jerseyEnvironment.register(new UserResource(dakiyaUserDetailsDAO, dakiyaRuntimeSettings,
                dakiyaUserDAO, configuration.getInstanceType()));
        jerseyEnvironment.register(new MetabaseResource(redshiftDao));

        environment.servlets().addFilter("DakiyaUserAccessLogger", new DakiyaUserAccessLogger())
                .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");

        final FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);

// Configure CORS parameters
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*"); // Allows requests from all domains
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "OPTIONS,GET,PUT,POST,DELETE,HEAD");

// Ensure credentials are supported if needed (e.g., cookies, authentication)
        cors.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, "true");

// Set max age if needed to improve performance by reducing preflight requests
        cors.setInitParameter(CrossOriginFilter.PREFLIGHT_MAX_AGE_PARAM, "1800"); // Example: 30 minutes

// Add URL mapping
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

// Other configurations
        registerExceptionMappers();
        log.info("DakiyaApplication initial setup complete for purpose " + configuration.getInstanceType());


    }

    private boolean initializeDBIs() {
        // database connection initialization
        final DBIFactory dbiFactory = new DBIFactory();

        final DBI dakiyaDBI = dbiFactory
                .build(environment, this.dakiyaConfiguration.getDakiyaDataSourceFactory(), "dakiya-db-postgres");
        final DBI metabaseDBI = dbiFactory
                .build(environment, this.dakiyaConfiguration.getRedshiftDataSourceFactory(), "metabase-db-postgres");

        log.info("make sure you are running postgres version 9.5 and above");
        DakiyaDBFactory.setDakiyaDB(dakiyaDBI);
        DakiyaDBFactory.setMetabaseDB(metabaseDBI);
        return true;

    }

    private boolean initializeDAOs() {
        // JDBI gets new handle and immediately release it barring the iterator based results and open transactions


        // Note: objects are themselves immutable however their references are not
        try {
            DBI dakiyaDBI = DakiyaDBFactory.getDakiyaDB();
            dakiyaDBI.setSQLLog(new PrintStreamLog());

            DBI metabaseDBI = DakiyaDBFactory.getMetabaseDB();

            final CampaignDAO campaignDAO = dakiyaDBI.onDemand(CampaignDAO.class);
            this.campaignDAO = campaignDAO;

            this.logDAO = dakiyaDBI.onDemand(LogDAO.class);

            final DakiyaUserDAO dakiyaUserDAO = dakiyaDBI.onDemand(DakiyaUserDAO.class);
            this.dakiyaUserDAO = dakiyaUserDAO;

            final DakiyaUserDetailsDAO dakiyaUserDetailsDAO = dakiyaDBI.onDemand(DakiyaUserDetailsDAO.class);
            this.dakiyaUserDetailsDAO = dakiyaUserDetailsDAO;

            final DakDAO dakDAO = dakiyaDBI.onDemand(DakDAO.class);
            this.dakDAO = dakDAO;


            final RedshiftDao redshiftDao = new RedshiftDao(metabaseDBI);
            this.redshiftDao = redshiftDao;

            return true;

        } catch (Exception e) {
            log.error(e.getMessage());
            return false;

        }
    }

    private boolean initializeScheduler() {

        java.util.Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver d = drivers.nextElement();
            if (d.getClass().getName().startsWith("com.amazon.redshift")) {
                try {
                    DriverManager.deregisterDriver(d);
                    DriverManager.registerDriver(d);
                } catch (SQLException e) {
                    log.warn("Could not deregister redshift driver" + e.getMessage());
                    return false;
                }
                break;
            }
        }
        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory(dakiyaConfiguration.getQuartzProperties());
            SchedulerManager schedulerManager = new SchedulerManager(schedulerFactory);

            environment.lifecycle().manage(schedulerManager);

            final SchedulerHealthCheck schedulerHealthCheck = new SchedulerHealthCheck();
            environment.healthChecks().register(DakiyaStrings.DAKIYA_SCHEDULER, schedulerHealthCheck);

            this.schedulerManager = schedulerManager;

            return true;
        } catch (Exception e) {
            log.error("Failed to initialize scheduler. Exception: " + e.getMessage() + "\n" +
                    "Limited functionality will be available");
            return false;
        }
    }

    private boolean initializeDakiyaRuntimeSettings() {
        try {
            DakiyaSettingDAO dakiyaSettingDAO = DakiyaDBFactory.getDakiyaDB().onDemand(DakiyaSettingDAO.class);
            DakiyaRuntimeSettings dakiyaRuntimeSettings = new DakiyaRuntimeSettings(dakiyaSettingDAO);

            // first save and/or update the settings
            if (dakiyaConfiguration.isResetDakiyaRuntimeSettings()) {
                dakiyaRuntimeSettings.saveSettingsFromHashMap(dakiyaConfiguration.getDakiyaRuntimeSettingsMap());
            }

            this.dakiyaRuntimeSettings = dakiyaRuntimeSettings;

            return true;

        } catch (SQLException ex) {
            log.error(ex.getMessage());
            return false;
        }
    }

    private void setupAuthentication() {

        jerseyEnvironment.register(new RBACFilter());
//        jerseyEnvironment.register(new AuthDynamicFeature(
//                new BasicCredentialAuthFilter.Builder<DakiyaUser>()
//                        .setAuthenticator(new DakiyaAuthenticator(dakiyaUserDAO))
//                        .setAuthorizer(new DakiyaAuthorizer())
//                        .setRealm(DakiyaStrings.DAKIYA_NAME)
//                        .buildAuthFilter()));
//        jerseyEnvironment.register(RolesAllowedDynamicFeature.class);
//        jerseyEnvironment.register(new AuthValueFactoryProvider.Binder<>(DakiyaUser.class));
    }

    private void startBackGroundTasksManager() {
        DakiyaBackGroundTasksManager dakiyaBackGroundTasksManager = new DakiyaBackGroundTasksManager(dakiyaConfiguration.getMaxDakiyaBackgroundTasks());
        environment.lifecycle().manage(dakiyaBackGroundTasksManager);
        final BackGroundTaskManagerHealthCheck backGroundTaskManagerHealthCheck = new BackGroundTaskManagerHealthCheck();
        environment.healthChecks().register(DakiyaStrings.DAKIYA_BACKGROUND_TASK_MANAGER, backGroundTaskManagerHealthCheck);
    }

    private void registerExceptionMappers() {
        jerseyEnvironment.register(new BadRequestExceptionMapper());
        jerseyEnvironment.register(new ForbiddenExceptionMapper());
        jerseyEnvironment.register(new InternalServerErrorMapper());
        jerseyEnvironment.register(new InvalidCampaignJsonExceptionMapper());
        jerseyEnvironment.register(new JdbiExceptionMapper());
        jerseyEnvironment.register(new NotFoundExeceptionMapper());
        jerseyEnvironment.register(new WebApplicationExceptionMapper());
        jerseyEnvironment.register(new SchedulerExceptionMapper());
        jerseyEnvironment.register(new IllegalArgumentExceptionMapper());
    }
}
