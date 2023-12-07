package com.untitled.ecm.dao;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

public interface DakiyaUserDAO {

    @SqlQuery("SELECT role FROM dakiya_users WHERE email = :email")
    String getRole(@Bind("email") String email);

    @SqlQuery("SELECT hashed_password FROM dakiya_users WHERE email = :email")
    String getHashedPasswordByEmail(@Bind("email") String email);

    @SqlUpdate("UPDATE dakiya_users " +
            "SET hashed_password = :hashed_password " +
            "WHERE email = :email;")
    int updateHashedPassword(@Bind("email") String email, @Bind("hashed_password") String hashedPassword);

    @SqlUpdate("UPDATE dakiya_users " +
            "SET role = :role " +
            "WHERE email = :email;")
    int updateRole(@Bind("email") String email, @Bind("role") String role);

    @SqlUpdate("INSERT INTO dakiya_users " +
            "(email, hashed_password, role) " +
            "VALUES(:email, :hashed_password, :role)")
    int createDakiyaUser(@Bind("email") String email, @Bind("hashed_password") String hashedPassword, @Bind("role") String role);

    void close();


}
