/**
 * This file is part of gui-serverbackend.
 *
 * gui-serverbackend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * gui-serverbackend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with gui-serverbackend.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.usu.research.hobbit.gui.util;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

/**
 * Created on 2017-07-31.
 */
public class IndexServer extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/index.html");

        HttpServletRequest wrapper = new HttpServletRequestWrapper(request) {
            public String getServletPath() { return ""; }
            public String getPathInfo() { return "/index.html"; }
        };

        dispatcher.forward(wrapper, response);
    }
}
