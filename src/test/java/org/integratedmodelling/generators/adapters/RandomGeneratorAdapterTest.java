package org.integratedmodelling.generators.adapters;

import org.integratedmodelling.klab.api.data.Data;
import org.integratedmodelling.klab.api.geometry.Geometry;
import org.integratedmodelling.klab.api.knowledge.Observable;
import org.integratedmodelling.klab.api.knowledge.Urn;
import org.integratedmodelling.klab.api.services.runtime.Notification;
import org.integratedmodelling.klab.configuration.ServiceConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RandomGeneratorAdapterTest {


    @BeforeAll
    public static void beforeAll() {
        // This sets up the k.LAB environment for the service side
        var ignored = ServiceConfiguration.INSTANCE.allowAnonymousUsage();
    }

    final String centralColombia =
            "τ0(1){ttype=LOGICAL,period=[1609459200000 1640995200000],tscope=1.0,"
                    + "tunit=YEAR}S2(934,631){bbox=[-75.2281407807369 -72.67107290964314 3.5641500380320963 5"
                    + ".302943221927137],"
                    + "shape"
                    + "=00000000030000000100000005C0522AF2DBCA0987400C8361185B1480C052CE99DBCA0987400C8361185B1480C052CE99DBCA098740153636BF7AE340C0522AF2DBCA098740153636BF7AE340C0522AF2DBCA0987400C8361185B1480,proj=EPSG:4326}";

    private boolean hasErrorNotification(List<Notification> notifications) {
        return notifications.stream()
                .anyMatch(n -> n.getLevel() == Notification.Level.Error
                    || n.getLevel() == Notification.Level.SystemError);
    }

    @Nested
    class SmokeTests {
//        @Test
//        @Disabled("To be fixed/analyzed: throwing a nullptr exception")
//        public void validateData() {
//            var observable = Observable.objects("porquerolles");
//            var geometry = Geometry.create(centralColombia);
//            var builder = Data.builder("colombia", observable, geometry);
//            var adapter = new RandomGeneratorAdapter();
//
//            adapter.encode(Urn.of("klab:random:data:poisson#p"), builder, geometry, observable, null);
//
//            var built = builder.build();
//            assertFalse(hasErrorNotification(built.notifications()));
//        }
//
//        @Test
//        @Disabled("Events is WIP")
//        public void validateEvent() {
//            var observable = Observable.objects("porquerolles");
//            var geometry = Geometry.create(centralColombia);
//            var builder = Data.builder("colombia", observable, geometry);
//            var adapter = new RandomGeneratorAdapter();
//
//            adapter.encode(Urn.of("klab:random:events:test"), builder, geometry, observable, null);
//
//            var built = builder.build();
//            assertFalse(hasErrorNotification(built.notifications()));
//        }
//
//        @Test
//        public void validateObject() {
//            var observable = Observable.objects("porquerolles");
//            var geometry = Geometry.create(centralColombia);
//            var builder = Data.builder("colombia", observable, geometry);
//            var adapter = new RandomGeneratorAdapter();
//
//            adapter.encode(Urn.of("klab:random:objects:polygons"), builder, geometry, observable, null);
//
//            var built = builder.build();
//            assertFalse(hasErrorNotification(built.notifications()));
//        }
    }

}