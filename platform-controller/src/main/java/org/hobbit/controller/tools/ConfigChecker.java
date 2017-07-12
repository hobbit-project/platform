package org.hobbit.controller.tools;

import org.hobbit.controller.config.HobbitConfig;
import org.hobbit.controller.config.HobbitConfig.TimeoutConfig;

/**
 * Small tool that reads the config file for making sure that it is readable and
 * does not contain syntax errors.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class ConfigChecker {

    public static void main(String[] args) throws Exception {
        // load HOBBIT config
        HobbitConfig cfg = HobbitConfig.loadConfig();
        for (String s : cfg.timeouts.keySet()) {
            TimeoutConfig t = cfg.getTimeout(s);
            System.out.println(s + " " + t.benchmarkTimeout + " " + t.challengeTimeout);
        }
        System.out.println("If this message is printed without any errors above the config seems to be readable.");
    }
}
