package com.example.legacy.web;

import com.example.legacy.db.DatabaseUtil;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Optional scheduler intentionally not started in the web tier; cron/Maven tasks are explicit.
 *
 * @author Chuck Emmons
 */
public final class ApplicationListener implements ServletContextListener {
  /**
   * Leaves database initialization lazy and does not start background jobs in the web tier.
   *
   * @param event the servlet context lifecycle event
   */
  @Override
  public void contextInitialized(ServletContextEvent event) {
    /* Deliberately lazy database initialization */
  }

  /**
   * Closes the shared Hibernate session factory when the application is undeployed or stopped.
   *
   * @param event the servlet context lifecycle event
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Override
  public void contextDestroyed(ServletContextEvent event) {
    DatabaseUtil.closeSessionFactory();
  }
}
