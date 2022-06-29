import stat
from turtle import up


AWMO = """
    @Test
    public void AWMO_test() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Thread t = new Thread(new CountDownRunnable(latch));
        t.start();
        Assert.assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
    }

"""

TMO = """
    @Test(timeout = 2)
    public void TMO_test() throws Exception {
        Thread.sleep(1);
    }
"""

UPMO = """
    @Test
    public void UPMO_test() {
        HashMap<String, String> map = new HashMap<String, String>();
        List<String> methodNames = new ArrayList<>();
        Method[] methods = map.getClass().getMethods();
        for (Method method : methods)
            methodNames.add(method.getName());
        Assert.assertEquals(methodNames.get(0), "remove");
    }
"""


RNCMO = """
@Test
    public void fileTest() {
        File file = new File("test.txt");

    }
"""

static_test = "FieldClass fieldClass = new FieldClass();"
static_class = """
class FieldClass {
    public static int i = 0;
}
"""

template_test ="""
    private static Server server;
    @Test
    public void serverTest() throws Exception {
        server = new Server(555);
        server.start();
    }"""


thread_test = """
@Test
    public void threadSleepTest() throws Exception {
        final int[] value = {1};

        System.out.println("Initial value: " + value[0]);

        Thread testThread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                value[0]++;
            }
        };

        testThread.start();

        // +99,99% probability of passing since 100ms is sufficient for such operation.
        Thread.sleep(2);
        // Always fail since testThread takes >10ms to run.
        //Thread.sleep(0);

        System.out.println("Final value: " + value[0]);
        Assert.assertEquals(2, value[0]);
    }

"""

timezone_test = """

@Test
    public void timeZoneDependencyTest() throws Exception {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // Otherwise, it'll take the default timezone.
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        Date date1 = null, date2 = null;
        date2 = dateFormat.parse("9111-04-05 02:02:02");

        date1 = dateFormat.parse("2020-11-22 10:13:55");
        date2 = dateFormat.parse("2020-11-22 10:13:55");


        Assert.assertNotNull(date1);
        Assert.assertNotNull(date2);
        Assert.assertEquals(date1, date2);
    }

"""


cacheTest = """
static Cache<String, String> cache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(100)
            .build();
    @Test
    public void cacheTest() {
        cache.put("a", "b");
    }


"""

class_1 = """
class CountDownRunnable extends  Thread {
    private CountDownLatch countDownLatch;
    public CountDownRunnable(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }
    @Override
    public void run() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        countDownLatch.countDown();
    }
}
"""

count_down_test = """
    @Test
    public void AWMO_test() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Thread t = new Thread(new CountDownRunnable(latch));
        t.start();
        Assert.assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
    }
"""

mockito_test = """
    @Mock
    static List mockk = mock(ArrayList.class);

    @Test
    public void testMockito() {
        mockk.add("one");
        mockk.add("two");
        /*mock.add("three");*/

        Mockito.verify(mockk, times(2))
                .add(anyString());
    }

"""
file_test = """
@Test
    public void testFile() throws IOException {
    FileOutputStream fos = null;
    File file;
    String mycontent = "This is my Data which needs" +
            " to be written into the file";
    //Specify the file path here
    file = new File("./myfile.txt");
    fos = new FileOutputStream("./myfile.txt");

    /* This logic will check whether the file
     * exists or not. If the file is not found
     * at the specified location it would create
     * a new file*/
    if (!file.exists()) {
        file.createNewFile();
    }

    /*String content cannot be directly written into
     * a file. It needs to be converted into bytes
     */
    byte[] bytesArray = mycontent.getBytes();

    fos.write(bytesArray);
    fos.flush();
    System.out.println("File Written Successfully");
}

"""

string_test = """
@Test
    public void testString() {
        Set mySet = new HashSet<>();

        mySet.add("a");
        mySet.add("b");
        mySet.add("c");

        System.out.println(mySet);

        Assert.assertEquals("[a, b, c]", mySet.toString());
    }
"""

database_test = """
    private static Connection con;
    private static boolean hasData = false;

    public void getConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        con = DriverManager.getConnection("jdbc:sqlite:testdb.db");
        initialize();
    }

    public void initialize() throws SQLException {
        try {
            Statement statement = con.createStatement();

            Statement statement1 = con.createStatement();
            statement1.execute("CREATE TABLE user(id integer, fName text, lName text, primary key(id));");
        } catch (SQLException throwables) {
            System.out.println("table already exists");
        }

        PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO user values(?, ?, ?)");
        preparedStatement.setString(2, "Alperen");
        preparedStatement.execute();
    }


    public static void createTable() {
        try {
            Statement statement1 = con.createStatement();
            statement1.execute("CREATE TABLE user(id integer, fName text, lName text, primary key(id));");
        } catch (SQLException throwables) {
            System.out.println("table already exists");
        }
    }

    @Test
    public void testDB() throws SQLException, ClassNotFoundException {
        if (con == null) {
            getConnection();
        }

        Statement statement = con.createStatement();
        ResultSet res = statement.executeQuery("SELECT fname, lname FROM user");
        res.close();
        statement.close();

        createTable();

        PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO user(id, fName, lName) values(?, ?, ?)");
        preparedStatement.setString(3, "Yıldız");
        preparedStatement.setString(2, "Alperen");
        preparedStatement.execute();
        preparedStatement.close();

        con.close();
    }

"""



imports = """
import java.sql.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.util.HashSet;
import org.junit.Assert;
import java.util.Set;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.io.FileOutputStream;
import org.eclipse.jetty.server.Server;
import java.util.Date;
import java.util.ArrayList;
import org.junit.Test;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import static org.mockito.Mockito.*;
import org.junit.Assert;
"""

dependency_1 = """
    <dependency>
      <groupId>org.eclipse.jetty</groupId>
      <artifactId>jetty-server</artifactId>
      <version>9.4.44.v20210927</version>
      <scope>test</scope>
    </dependency>
"""

dependency_2 = """
    <dependency>
      <groupId>org.mockito</groupId>
      <artifactId>mockito-all</artifactId>
      <version>1.10.19</version>
      <scope>test</scope>
    </dependency>
    """

templates = [TMO, UPMO, RNCMO, thread_test, timezone_test, count_down_test, cacheTest, mockito_test, file_test, string_test, database_test, static_test]

classes = [
    """
    class CountDownRunnable extends  Thread {
    private CountDownLatch countDownLatch;
    public CountDownRunnable(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }
    @Override
    public void run() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        countDownLatch.countDown();
    }
}
    """, 
    static_class
]

dependencies = [dependency_1, dependency_2]