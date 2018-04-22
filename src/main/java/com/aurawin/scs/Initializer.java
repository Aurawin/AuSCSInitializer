package com.aurawin.scs;

import com.aurawin.core.Environment;
import com.aurawin.core.lang.Database;
import com.aurawin.core.lang.Table;
import com.aurawin.core.stored.Dialect;
import com.aurawin.core.stored.Driver;
import com.aurawin.core.stored.Manifest;
import com.aurawin.core.stored.entities.security.Certificate;
import com.aurawin.scs.audisk.AuDisk;
import com.aurawin.scs.solution.Namespace;
import com.aurawin.scs.solution.Settings;
import com.aurawin.scs.stored.Entities;
import com.aurawin.scs.stored.bootstrap.Bootstrap;
import com.aurawin.scs.stored.cloud.*;
import com.aurawin.scs.stored.domain.Domain;
import com.aurawin.scs.stored.domain.user.Account;

import static com.aurawin.core.stored.entities.Entities.CascadeOff;
import static com.aurawin.scs.Initializer.Cert.*;

public class Initializer {


    public static Domain domain;
    public static Location lc;
    public static Group gp;
    public static Resource rcPhoenix;
    public static Resource rcChump;
    public static Node nPhoenix;
    public static Node nChump;
    public static Node nAu1;
    public static Node nAu2;
    public static Node nDisk;

    public static Account account;
    public static Service svcHTTP;
    public static Service svcAUDISK;
    public static Disk auDisk;
    public static Certificate Cert ;

    // todo JSON Config file as input to system config

    public static final String Mount = "/media/raid/AuDisk";
    public static final String DomainName = "aurawin.com";
    public class Root {
        public class Environment{
            public static final String Name = "COM_AURAWIN_ROOT_NAME";
            public static final String Password = "COM_AURAWIN_ROOT_PASSWORD";
        }
    }
    public class Cert{
        public static final String orgUnit = "NOC";
        public static final String orgName = "Aurawin LLC";
        public static final String orgStreet = "19309 Stage Line Trl";
        public static final String orgLocality = "Pflugerville";
        public static final String orgState = "TX";
        public static final String orgPostal = "78660";
        public static final String orgCountry = "US";
        public static final String orgEmail = "support@auraiwn.com";
        public static final int Duration = 365;
    }
    public class Cloud{
        public class Cluster{
            public static final String Name = "Office";
            public static final String Rack = "1";
            public static final String Row = "1";
        }
        public class Location{
            public static final String Area = "Falcon Pointe";
            public static final String Building = "19309";
            public static final String Street = "Stage Line Trail";
            public static final String Floor = "1";
            public static final String Room = "Office";
            public static final String Locality = "Pflugerville";
            public static final String Region = "Texas";
            public static final String Postal = "78660";
            public static final String Country = "USA";
        }
    }
    public static void databaseCreateOrOverwrite() throws Exception{
        com.aurawin.core.solution.Settings.Initialize(
                "AuSCS",
                "Aurawin Social Computing Server",
                "Universal"
                );
        Manifest mf = new Manifest(
                Environment.getString(Table.DBMS.Username),                     // username
                Environment.getString(Table.DBMS.Password),                     // password
                Environment.getString(Table.DBMS.Host),                         // host
                Environment.getInteger(Table.DBMS.Port),                        // port
                com.aurawin.core.lang.Database.Config.Automatic.Commit.On,      // autocommit
                2,                                                   // Min Poolsize
                20,                                                 // Max Poolsize
                1,                                                 // Pool Acquire Increment
                50,                                               // Max statements
                10,                                                     // timeout
                Database.Config.Automatic.Create,                               //
                "HTTPServerTest",                                      // database
                Dialect.Postgresql.getValue(),                                  // Dialect
                Driver.Postgresql.getValue(),                                   // Driver
                Bootstrap.buildAnnotations()
        );

        Entities.Initialize(mf);



}
    public static void databaseSetupEntities() throws Exception {
        Cert = Certificate.createSelfSigned(
                "*."+DomainName,
                orgUnit,
                orgName,
                orgStreet,
                orgLocality,
                orgState,
                orgPostal,
                orgCountry,
                orgEmail,
                Duration
        );
        Entities.Save(Cert, CascadeOff);

        lc = Bootstrap.Cloud.addLocation(
                Cloud.Location.Area,
                Cloud.Location.Building,
                Cloud.Location.Street,
                Cloud.Location.Floor,
                Cloud.Location.Room,
                Cloud.Location.Locality,
                Cloud.Location.Region,
                Cloud.Location.Postal,
                Cloud.Location.Country
        );
        gp = Bootstrap.Cloud.addGroup(
                lc,
                Cloud.Cluster.Name,
                Cloud.Cluster.Rack,
                Cloud.Cluster.Row
        );

        // each physical computer should be added here.
        rcPhoenix = Bootstrap.Cloud.addResource(gp, "Phoenix");
        rcChump = Bootstrap.Cloud.addResource(gp, "Chump");

        // each local ip ( if any ) can be added here
        nPhoenix = Bootstrap.Cloud.addNode(rcPhoenix, "phoenix", "172.16.1.1");
        nChump = Bootstrap.Cloud.addNode(rcChump, "chump", "172.16.1.2");

        // each public ip can be added here
        nAu1 = Bootstrap.Cloud.addNode(rcPhoenix, "au1", "107.218.165.193");
        nAu2 = Bootstrap.Cloud.addNode(rcChump, "au2", "107.218.165.194");

        // variable to hold which node that will host the actual disk array
        nDisk = nPhoenix;

        // add cloud service that will run the AuDisk service
        svcAUDISK = Bootstrap.Cloud.addService(
                nDisk,
                Namespace.Stored.Cloud.Service.AUDISK,
                Settings.Stored.Cloud.Service.Port.AuDisk,
                1,
                10,
                1
        );
        // add cloud service that will run HTTP service
        svcHTTP = Bootstrap.Cloud.addService(
                nPhoenix,
                Namespace.Stored.Cloud.Service.HTTP,
                1080,
                1,
                10,
                1
        );
        // add actual disk
        auDisk = Bootstrap.Cloud.addDisk(nPhoenix, svcAUDISK, Mount);
        AuDisk.Initialize(nDisk, Cert);

        domain = Bootstrap.addDomain(DomainName);
        account = Bootstrap.addUser(
                domain,
                Environment.getString(Root.Environment.Name),
                Environment.getString(Root.Environment.Password),
                Namespace.Stored.Domain.User.Role.Admin
        );

        // Set the domain of the nodes (to map IPs)
        nPhoenix.setDomain(domain);
        nChump.setDomain(domain);
        nAu1.setDomain(domain);
        nAu2.setDomain(domain);

        // initialize Plugin
        com.aurawin.scs.rsr.protocol.http.Server.Bootstrap();
    }

    public static void main(String[] args) {

    }
}
