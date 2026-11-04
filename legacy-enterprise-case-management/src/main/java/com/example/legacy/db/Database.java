package com.example.legacy.db;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;
public final class Database {
    public interface Work<T> { T run(Session s); }
    private static volatile SessionFactory factory;
    private Database() {}
    public static synchronized SessionFactory factory() {
        if (factory == null) {
            Configuration cfg = new Configuration().configure("hibernate.cfg.xml");
            override(cfg, "DB_URL", "hibernate.connection.url");
            override(cfg, "DB_USER", "hibernate.connection.username");
            override(cfg, "DB_PASSWORD", "hibernate.connection.password");
            factory = cfg.buildSessionFactory();
        }
        return factory;
    }
    private static void override(Configuration cfg, String env, String property) {
        String value = System.getenv(env);
        if (value != null && !value.isEmpty()) cfg.setProperty(property, value);
    }
    public static <T> T tx(Work<T> job) {
        Session s = factory().openSession(); Transaction tx = null;
        try {
            tx = s.beginTransaction();
            T result = job.run(s);
            tx.commit(); return result;
        } catch (RuntimeException e) {
            if (tx != null) try { tx.rollback(); } catch (RuntimeException ignored) { }
            throw e;
        } finally { s.close(); }
    }
    public static synchronized void shutdown() {
        if (factory != null) { factory.close(); factory = null; }
    }
}
