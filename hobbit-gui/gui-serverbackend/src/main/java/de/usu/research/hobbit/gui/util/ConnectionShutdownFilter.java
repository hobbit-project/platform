package de.usu.research.hobbit.gui.util;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import de.usu.research.hobbit.gui.rabbitmq.RabbitMQConnectionSingleton;

public class ConnectionShutdownFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {
		filterChain.doFilter(request, response);
	}

	@Override
	public void destroy() {
		RabbitMQConnectionSingleton.shutdown();
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}
}