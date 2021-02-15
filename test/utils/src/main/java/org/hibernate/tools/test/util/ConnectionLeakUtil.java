/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 * 
 * Copyright 2018-2020 Red Hat, Inc.
 *
 * Licensed under the GNU Lesser General Public License (LGPL), 
 * version 2.1 or later (the "License").
 * You may not use this file except in compliance with the License.
 * You may read the licence in the 'lgpl.txt' file in the root folder of 
 * project or obtain a copy at
 *
 *     http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tools.test.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.opentest4j.AssertionFailedError;

public class ConnectionLeakUtil {
	
	public static ConnectionLeakUtil forH2() {
		ConnectionLeakUtil result = new ConnectionLeakUtil();
		result.idleConnectionCounter = new H2IdleConnectionCounter();
		return result;
	}
	
	private IdleConnectionCounter idleConnectionCounter = null;
	
	private int connectionCount = 0;
	
	public void initialize() {
		connectionCount = idleConnectionCounter.countConnections();
	}
	
	public void assertNoLeaks() {
		int leaked = getLeakedConnectionCount();
		if (leaked != 0) {
			throw new AssertionFailedError(leaked + " connections are leaked.");
		}
	}
	
	private int getLeakedConnectionCount() {
		int previousCount = connectionCount;
		connectionCount = idleConnectionCounter.countConnections();
		return connectionCount - previousCount;
	}
	
	private static interface IdleConnectionCounter {
		int countConnections();
	}
	
	private static class H2IdleConnectionCounter implements IdleConnectionCounter {
		private Connection newConnection() {
			try {
				return DriverManager.getConnection("jdbc:h2:mem:test", "sa", "");
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		public int countConnections() {		
			try {
				int result = 0;
				Connection connection = newConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(
						"SELECT COUNT(*) " +
						"FROM information_schema.sessions " + 
						"WHERE statement IS NULL");
				while (resultSet.next()) {
					result = resultSet.getInt(1);
				}
				statement.close();
				connection.close();
				return result;
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		
	}

}
