package com.infraestrctura.synthevia.repositorio;

import com.infraestrctura.synthevia.entidad.Reporte;
import com.infraestrctura.synthevia.entidad.ViaSegmento;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import static org.junit.jupiter.api.Assertions.*;

class ReporteConsultasTest {
    @Test
    void validaRelacionBusquedaYConteoPaginadoSinConectarALaBase() {
        // Valida las consultas JPQL con Hibernate real, sin credenciales ni red.
        Configuration configuration = new Configuration()
                .addAnnotatedClass(Reporte.class)
                .addAnnotatedClass(ViaSegmento.class)
                .setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .setProperty("hibernate.boot.allow_jdbc_metadata_access", "false")
                .setProperty("hibernate.hbm2ddl.auto", "none");
        try (var factory = configuration.buildSessionFactory(); var em = factory.createEntityManager()) {
            assertNotNull(new JpaRepositoryFactory(em).getRepository(ReporteRepository.class));
        }
    }
}
