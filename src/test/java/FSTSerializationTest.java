import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import org.testng.annotations.Test;

// Test case, based on examples by @TheHound and @andrewl102
public class FSTSerializationTest {

    private final FSTConfiguration fstConf = FSTConfiguration.createDefaultConfiguration();

    public static void main(String[] args) throws InterruptedException, ClassNotFoundException {
        new FSTSerializationTest().tryAndReProduceBug();
    }

    @Test
    public void tryAndReProduceBug() throws InterruptedException, ClassNotFoundException {
        final Serializable testObject = new TestObject();
        ExecutorService pool = Executors.newFixedThreadPool( 16 );

        // Pre-register classes to work around https://github.com/RuedigerMoeller/fast-serialization/issues/235
        fstConf.registerClass( DateTime.class );
        fstConf.registerClass( LocalDate.class );
        fstConf.registerClass( Class.forName( "org.joda.time.chrono.ISOChronology$Stub" ) );
        fstConf.registerClass( Class.forName( "org.joda.time.DateTimeZone$Stub" ) );
        fstConf.registerClass( Class.forName( "FSTSerializationTest$TestObject" ) );

        List<Future<?>> futures = new ArrayList<>();

        for ( int i = 0; i < 1_000_000; i++ ) {
            futures.add( pool.submit( () -> {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                try {
                    writeObject( testObject, stream );
                    readObject( new ByteArrayInputStream( stream.toByteArray() ) );
                }
                catch ( Exception e ) {
                    System.err.println( "ERROR" + e );
                    throw new RuntimeException( e );
                }

            } ) );
        }
        pool.shutdown();
        pool.awaitTermination( 10, TimeUnit.HOURS );
        for ( Future<?> future : futures ) {
            try {
                Object object = future.get();
            }
            catch ( Exception e ) {
                e.printStackTrace();
                throw new RuntimeException( e );
            }
        }
    }

    public Object readObject( final InputStream inputStream ) throws IOException {
        try ( FSTObjectInput in = new FSTObjectInput( inputStream, fstConf ) ) {
            return in.readObject();
        }
        catch ( Exception e ) {
            throw new IOException( e );
        }
    }

    public void writeObject( final Object obj, final OutputStream outputStream ) throws IOException {
        try ( FSTObjectOutput out = new FSTObjectOutput( outputStream, fstConf ) ) {
            out.writeObject( obj );
        }
        catch ( Exception e ) {
            throw new IOException( e );
        }
    }

    public static final class TestObject implements Serializable {
        private static final long serialVersionUID = -9083923722080054771L;

        private String string = "asdasdassd";

        private DateTime dateTime = new DateTime();

        private LocalDate locaTime = new LocalDate();

        private int inty = 129;
    }
}