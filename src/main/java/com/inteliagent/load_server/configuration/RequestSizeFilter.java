package com.inteliagent.load_server.configuration;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
@Component
public class RequestSizeFilter extends OncePerRequestFilter {
    private static final int MAX = 2 * 1024 * 1024;
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request.getMethod().equals("POST") || request.getMethod().equals("PUT"))) { chain.doFilter(request,response); return; }
        if (request.getContentLengthLong() > MAX) { response.setStatus(413); return; }
        byte[] bytes = request.getInputStream().readNBytes(MAX + 1);
        if (bytes.length > MAX) { response.setStatus(413); return; }
        var wrapped = new HttpServletRequestWrapper(request) {
            public ServletInputStream getInputStream() {
                var stream = new ByteArrayInputStream(bytes);
                return new ServletInputStream() {
                    public int read() { return stream.read(); }
                    public boolean isFinished() { return stream.available()==0; }
                    public boolean isReady() { return true; }
                    public void setReadListener(ReadListener listener) { throw new UnsupportedOperationException(); }
                };
            }
        };
        chain.doFilter(wrapped,response);
    }
}
