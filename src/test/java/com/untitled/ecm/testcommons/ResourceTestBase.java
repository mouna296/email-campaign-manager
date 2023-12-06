package com.untitled.ecm.testcommons;

import com.untitled.ecm.DakiyaApplication;
import com.untitled.ecm.DakiyaConfiguration;
import com.untitled.ecm.constants.DakiyaStrings;
import com.untitled.ecm.constants.Roles;
import com.untitled.ecm.core.DakiyaRuntimeSettings;
import com.untitled.ecm.core.DakiyaUtils;
import com.untitled.ecm.dao.DakiyaDBFactory;
import com.untitled.ecm.dao.DakiyaSettingDAO;
import com.untitled.ecm.dao.DakiyaUserDAO;
import com.untitled.ecm.dao.DakiyaUserDetailsDAO;
import com.untitled.ecm.dtos.DakEmail;
import com.untitled.ecm.services.mail.InMemoryMailer;
import com.fasterxml.jackson.datatype.joda.JodaMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.google.common.collect.ImmutableSet;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeZone;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.skife.jdbi.v2.Batch;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.util.StringColumnMapper;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

import static org.junit.Assert.*;

public class ResourceTestBase {
    public static final String DUMMY_METABASE_DUMMY_COOLUMN_NAME = "email";
    protected static final String DUMMY_METABASE_DUMMY_TABLE_NAME = "metabase_dummy_table";
    protected static final ImmutableSet<String> dummyEmailsInMetabase = getDummyEmailsSet(100);
    private static final String CONFIG_PATH = ResourceHelpers.resourceFilePath("dakiya-test.yaml");
    private static final Set<String> validRoles = getValidRoles();
    @ClassRule
    public static DropwizardAppRule<DakiyaConfiguration> RULE = new DropwizardAppRule<>(DakiyaApplication.class, CONFIG_PATH);
    protected static ImmutableSet<String> sendridDomains;
    protected static DakiyaTestUser dakiyaTestUser;
    private static Client client;
    private static String basePath;
    private static DakiyaUserDAO dakiyaUserDAO;
    private static DakiyaUserDetailsDAO dakiyaUserDetailsDAO;

    @BeforeClass
    public static void beforeClass() throws Exception {
        assertNotNull(DakiyaDBFactory.getDakiyaDB());
        assertNotNull(DakiyaDBFactory.getMetabaseDB());
        setupBasePathString();

        client = ClientBuilder.newClient();
        JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
        provider.setMapper(new JodaMapper().setTimeZone(DateTimeZone.forID(DakiyaStrings.DAKIYA_TIMEZONE_INDIA).toTimeZone()));
        client.register(provider);

        DBI dakiyaDBI = DakiyaDBFactory.getDakiyaDB();

        dakiyaUserDAO = dakiyaDBI.onDemand(DakiyaUserDAO.class);
        dakiyaUserDetailsDAO = dakiyaDBI.onDemand(DakiyaUserDetailsDAO.class);
        resetDB();
    }

    protected static void resetDB() throws Exception {
        dropPostgressTables();
        refreshDakiyaRuntimeSettingsInDb();
        populateDummyMetabase();
        resetInMemorySentMailRecords();
        fetchAndPopulateSendgridDomains();
        dakiyaTestUser = createDakiyaTestUser(Roles.CAMPAIGN_SUPERVISOR);
    }

    private static void setupBasePathString() {
        String urlPattern = RULE.getEnvironment().jersey().getUrlPattern();
        if (urlPattern.endsWith("*")) {
            urlPattern = StringUtils.removeEndIgnoreCase(urlPattern, "*");
        }
        basePath = String.format("http://localhost:%d" + urlPattern, RULE.getLocalPort());
    }

    private static void refreshDakiyaRuntimeSettingsInDb() throws SQLException {
        // updated settings in db manually since reset-dakiya-runtime-settings: yes won't work because
        // at app start settings table may not always exist
        // in test.yaml reset-dakiya-runtime-settings has been turned off to ensure RULE gets initialized in all cases
        final DakiyaSettingDAO dakiyaSettingDAO = DakiyaDBFactory.getDakiyaDB().onDemand(DakiyaSettingDAO.class);
        final DakiyaRuntimeSettings dakiyaRuntimeSettings = new DakiyaRuntimeSettings(dakiyaSettingDAO);
        dakiyaRuntimeSettings.saveSettingsFromHashMap(RULE.getConfiguration().getDakiyaRuntimeSettingsMap());
    }

    private static void dropPostgressTables() throws Exception {
        RULE.getApplication().run("dakiya-db", "drop-all", "--confirm-delete-everything", CONFIG_PATH);
        RULE.getApplication().run("dakiya-db", "migrate", CONFIG_PATH);
    }

    private static void resetInMemorySentMailRecords() throws NoSuchFieldException, IllegalAccessException {
        Field field = InMemoryMailer.class.getDeclaredField("SENT_MAILS_RECORDS");
        boolean accessible = field.isAccessible();
        field.setAccessible(true);
        field.set(null, new HashMap<>());
        field.setAccessible(accessible);
    }


    @AfterClass
    public static void afterClass() {
        client.close();
    }


    private static ImmutableSet<String> getValidRoles() {
        List<String> validRoles = new ArrayList<>();
        try {
            Class consumers = Class.forName("com.untitled.ecm.constants.Roles");

            Field[] fields = consumers.getDeclaredFields();

            for (Field field : fields) {
                validRoles.add((String) field.get(null));
            }
        } catch (Exception e) {

        }
        return ImmutableSet.copyOf(validRoles);
    }

    private static void populateDummyMetabase() throws SQLException {
        DBI metabaseDB = DakiyaDBFactory.getMetabaseDB();
        Handle handle = metabaseDB.open();
        handle.execute(String.format("drop table if exists %s", DUMMY_METABASE_DUMMY_TABLE_NAME));
        handle.execute(String.format("CREATE TABLE %s (%s VARCHAR(255));",
                DUMMY_METABASE_DUMMY_TABLE_NAME,
                DUMMY_METABASE_DUMMY_COOLUMN_NAME));

        Batch batch = handle.createBatch();
        for (String email : dummyEmailsInMetabase) {
            batch.add(String.format("insert into %s (%s) values(\'%s\');",
                    DUMMY_METABASE_DUMMY_TABLE_NAME,
                    DUMMY_METABASE_DUMMY_COOLUMN_NAME,
                    email));
        }
        batch.execute();
        String count = handle
                .createQuery(String.format("select count(*) from %s",
                        DUMMY_METABASE_DUMMY_TABLE_NAME))
                .map(StringColumnMapper.INSTANCE)
                .first();
        assertEquals(dummyEmailsInMetabase.size(), Integer.parseInt(count));
        handle.close();
    }

    private static ImmutableSet<String> getDummyEmailsSet(final int count) {
        Set<String> set = new HashSet<>();
        for (int i = 0; i < count; i++) {
            set.add(UUID.randomUUID().toString() + "@gmail.com");
        }
        return ImmutableSet.copyOf(set);
    }

    static void fetchAndPopulateSendgridDomains() {
        try {
            DakiyaSettingDAO dao = DakiyaDBFactory.getDakiyaDB().onDemand(DakiyaSettingDAO.class);
            DakiyaRuntimeSettings runtimeSettings = new DakiyaRuntimeSettings(dao);
            List<String> domain = runtimeSettings.getAllSendGridDomains();
            sendridDomains = ImmutableSet.copyOf(runtimeSettings.getAllSendGridDomains());
            assertEquals(domain.size(), sendridDomains.size());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        assertNotNull(sendridDomains);
    }

    public static DakiyaTestUser createDakiyaTestUser(String role) {
        DakiyaTestUser testUser = createDakiyaTestUser_(role);
        assertNotNull(testUser);
        assertNotNull(testUser.getRole());
        assertNotNull(testUser.getFirstName());
        assertNotNull(testUser.getLastName());
        assertNotNull(testUser.getEmail());
        return testUser;
    }

    static DakiyaTestUser createDakiyaTestUser_(String role) {

        assertTrue(validRoles.contains(role));

        final DakiyaTestUser dakiyaTestUser = DakiyaTestUser
                .builder()
                .role(role)
                .email(UUID.randomUUID().toString() + "@gmail.com")
                .password(RandomStringUtils.randomAlphanumeric(6, 20))
                .build();
        // try catch to avoid adding throws clause to every test case
            // a rest endpoint for creating user does not (and likely never will) exist
            saveDakiyaTestUserInDb(dakiyaTestUser);
        return dakiyaTestUser;
    }

    private static void saveDakiyaTestUserInDb(DakiyaTestUser testUser) {
        dakiyaUserDAO.createDakiyaUser(testUser.getEmail(),
                DakiyaUtils.getBcryptHashedString(testUser.getPassword()), testUser.getRole());
        dakiyaUserDetailsDAO.createDakiyaUserDetails(testUser.getFirstName(),
                testUser.getLastName(), testUser.getEmail());
    }

    public Response makeGetRequest(final DakiyaTestUser testUser, final String resource) {
        return client
                .target(basePath + resource)
                .request()
                .header("Authorization", getHTTPBasicAuthorizationHeaderValue(testUser))
                .get();
    }

    public Response makePostRequest(final DakiyaTestUser testUser, final String resource, final Object object) {
        return client
                .target(basePath + resource)
                .request()
                .header("Authorization", getHTTPBasicAuthorizationHeaderValue(testUser))
                .post(Entity.json(object));
    }

    protected void ensureMailRecordContainsRecipient(InMemoryMailer.MailRecord mailRecord, String recipient, Instant maxKnowInstantBeforeSendingMail, int mailSentCount) {
        assertNotNull(mailRecord);
        Map<Instant, DakEmail> recipients = mailRecord.getRecipients();
        int count = 0;
        for (Map.Entry<Instant, DakEmail> entry : recipients.entrySet()) {
            if (maxKnowInstantBeforeSendingMail.isBefore(entry.getKey()) && entry.getValue().getEmail().equals(recipient)) {
                count++;
            }
        }
        assertEquals(mailSentCount, count);
    }

    String getHTTPBasicAuthorizationHeaderValue(DakiyaTestUser testUser) {
        assertNotNull(testUser.getEmail());
        assertNotNull(testUser.getPassword());
        return "Basic " + Base64.getEncoder().encodeToString((testUser.getEmail() + ":" +
                testUser.getPassword()).getBytes());
    }

}
