package gcs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J;

import java.io.PrintStream;
import java.util.TimeZone;


@EnableScheduling
@SpringBootApplication
public class GcsApplication implements CommandLineRunner {

	static Logger rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

	@Autowired
	private Environment environment;

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
		SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();
		SpringApplication.run(GcsApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		// 重定向 System.out 和 System.err
		// System.setOut(createOutPrintStream(System.out));
		// System.setErr(createErrPrintStream(System.err));
		// 获取应用的端口号
		String port = environment.getProperty("server.port", "8080");
		// 获取应用的上下文路径
		String contextPath = environment.getProperty("server.servlet.context-path", "");
		if (contextPath.endsWith("/")) {
			contextPath = contextPath.substring(0, contextPath.length() - 1);
		}
		System.out.println("项目启动成功: http://127.0.0.1:" + port + contextPath);
	}

	// 重定向 System.out
	private static PrintStream createOutPrintStream(final PrintStream printStream) {
		PrintStream outStream= new PrintStream(printStream) {
			public void print(final String string) {
				rootLogger.info(string);
			}
		};
		return outStream;
	}

	// 重定向 System.err
	private static PrintStream createErrPrintStream(final PrintStream printStream) {
		PrintStream outStream= new PrintStream(printStream) {
			public void print(final String string) {
				rootLogger.error(string);
			}
		};
		return outStream;
	}
}