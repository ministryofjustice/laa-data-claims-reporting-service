//package uk.gov.justice.laa.dstew.claimsreports.runner;
//
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.Query;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.context.ConfigurableApplicationContext;
//
//import java.util.List;
//
//import static org.mockito.Mockito.*;
//
//class ClaimsReportingServiceRunnerTest {
//
//  private EntityManager entityManager;
//  private ConfigurableApplicationContext context;
//  private ClaimsReportingServiceRunner runner;
//  private Query query;
//
//  @BeforeEach
//  void setUp() {
//    entityManager = mock(EntityManager.class);
//    context = mock(ConfigurableApplicationContext.class);
//    query = mock(Query.class);
//
//    runner = new ClaimsReportingServiceRunner(entityManager, context);
//  }
//
//  @Test
//  void shouldQueryTablesAndCloseContext() {
//    // given
//    when(entityManager.createNativeQuery(anyString())).thenReturn(query);
//    when(query.getResultList()).thenReturn(List.of("table_a", "table_b"));
//
//    // when
//    runner.run(mock(ApplicationArguments.class));
//
//    // then
//    verify(entityManager).createNativeQuery(contains("information_schema.tables"));
//    verify(query).getResultList();
//    verify(context).close();
//  }
//
//  @Test
//  void shouldHandleEmptyResultSetGracefully() {
//    when(entityManager.createNativeQuery(anyString())).thenReturn(query);
//    when(query.getResultList()).thenReturn(List.of());
//
//    runner.run(mock(ApplicationArguments.class));
//
//    verify(query).getResultList();
//    verify(context).close();
//  }
//
//  @Test
//  void shouldHandleDatabaseErrorGracefully() {
//    when(entityManager.createNativeQuery(anyString())).thenThrow(new RuntimeException("DB error"));
//
//    runner.run(mock(ApplicationArguments.class));
//
//    verify(context).close(); // even after error, context should be closed
//  }
//}