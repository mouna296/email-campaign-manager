package com.untitled.ecm.dao;

import com.untitled.ecm.dao.mappers.DakiyaUserDetailsMapper;
import com.untitled.ecm.dtos.DakiyaUserDetails;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;

@RegisterMapper(DakiyaUserDetailsMapper.class)
public interface DakiyaUserDetailsDAO {
    @SqlQuery("SELECT dakiya_users_details.*, dakiya_users.role " +
            "FROM dakiya_users_details, dakiya_users " +
            "WHERE dakiya_users_details.email = dakiya_users.email")
    List<DakiyaUserDetails> findAll();

    @SqlQuery("SELECT dakiya_users_details.*, dakiya_users.role " +
            "FROM dakiya_users_details JOIN dakiya_users " +
            "ON dakiya_users_details.email = dakiya_users.email " +
            "WHERE dakiya_users.email = :email")
    DakiyaUserDetails findByEmail(@Bind("email") String email);

    @SqlUpdate("INSERT INTO dakiya_users_details " +
            "(first_name, last_name, email) " +
            "VALUES(:first_name, :last_name, :email) ")
    int createDakiyaUserDetails(@Bind("first_name") String firstName, @Bind("last_name") String lastName,
                                @Bind("email") String email);

    @SqlUpdate("UPDATE dakiya_users_details " +
            "SET (first_name, last_name)=(:first_name, :last_name) " +
            "WHERE email = :email")
    int updateDakiyaUserDetails(@Bind("email") String email, @Bind("first_name") String firstName,
                                @Bind("last_name") String lastName);

    void close();
}
