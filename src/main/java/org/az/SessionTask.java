package org.az;

import com.jcraft.jsch.Session;

/**
 * created by yangtong on 2024/6/15 12:01:52
 */
@FunctionalInterface
public interface SessionTask {

    void doTask(Session session);

}
