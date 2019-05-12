package com.sebnic.demo.fakeDNS;

import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.junit.Before;
import org.junit.Test;
import com.sun.jndi.dns.DnsContextFactory;

public class FakeDNSTest {

    private DirContext dirContext;

    private FakeDNS fakeDNS;

    @Before
    public void init() throws NamingException, InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(new Runnable() {
            public void run() {
                fakeDNS = new FakeDNS("127.0.0.1",12898);
                try {
                    fakeDNS.run();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread.sleep(2000);
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, DnsContextFactory.class.getName());
        env.put(Context.PROVIDER_URL, "dns://127.0.0.1:12898");
        dirContext = new InitialDirContext(env);
    }

    @Test
    public void test() throws NamingException, InterruptedException {
        Attributes attributes = dirContext.getAttributes("smtp.orange.com", new String[]{"MX"});
    }
}
