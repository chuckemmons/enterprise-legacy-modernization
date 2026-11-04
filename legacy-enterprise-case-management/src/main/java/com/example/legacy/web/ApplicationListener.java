package com.example.legacy.web;
import javax.servlet.*;
import com.example.legacy.db.Database;
/** Optional scheduler intentionally not started in the web tier; cron/Ant tasks are explicit. */
public final class ApplicationListener implements ServletContextListener {
 public void contextInitialized(ServletContextEvent e) { /* Deliberately lazy database initialization */ }
 public void contextDestroyed(ServletContextEvent e) {Database.shutdown();}
}
