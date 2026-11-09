package co.konradlorenz.abastecimiento;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@EnableScheduling
@SpringBootApplication
public class AbastecimientoApplication { public static void main(String[] a) { SpringApplication.run(AbastecimientoApplication.class, a); } }
