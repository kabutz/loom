/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 *
 * @summary Test verifies set/get TLS data and verifies it's consistency.
 * Test set TLS with thread name which it belongs to and verify this information when getting test.
 *  -- cbThreadStart
 *  -- by AgentThread
 *
 * Test doesn't verify that TLS is not NULL because for some threads TLS is not initialized initially.
 * TODO:
 *  -- verify that TLS is not NULL (not possible to do with jvmti, ThreadStart might be called too late)
 *  -- add more events where TLS is set *first time*, it is needed to test lazily jvmtThreadState init
 *  -- support virtual threads
 *  -- set/get TLS from other JavaThreads (not from agent and current thread)
 *  -- set/get for suspened (blocked?) threads
 *  -- split test to "sanity" and "stress" version
 *  -- update properties to run jvmti stress tests non-concurrently?
 *
 *
 * @library /test/lib
 * @run main/othervm/native -agentlib:SetGetThreadLocalStorageStress SetGetThreadLocalStorageStressTest
 */


import jdk.test.lib.jvmti.DebugeeClass;


public class SetGetThreadLocalStorageStressTest extends DebugeeClass {

    static {
        System.loadLibrary("SetGetThreadLocalStorageStress");
    }


    static int status = DebugeeClass.TEST_PASSED;

    public static void main(String argv[]) throws InterruptedException {
        int size = 10;
        int threadNum = Runtime.getRuntime().availableProcessors();
        if (argv.length > 0) {
            size = Integer.parseInt(argv[0]);
        }

        // need to sync start with agent thread only when main is started
        checkStatus(status);

        long uniqID = 0;
        for (int c = 0; c < size; c++) {
            Thread[] threads = new Thread[threadNum];
            for (int i = 0; i < threadNum; i++) {
                TaskMonitor task = new TaskMonitor();
                threads[i] = Thread.builder()
                        .task(task)
                        .name("TestedThread-" + uniqID++)
                        // TODO add virtual testing
                        //   .virtual()
                        .build();
            }

            for (Thread t : threads) {
                t.start();
            }

            for (Thread t : threads) {
                t.join();
            }
        }
    }
}


class TaskMonitor implements Runnable {
    public Object startingMonitor = new Object();
    public Object runningMonitor = new Object();
    public Object endingMonitor = new Object();

    // run thread continuously
    public void run() {
        // notify about starting
        synchronized (startingMonitor) {
            startingMonitor.notifyAll();
        }

        // notify about running
        synchronized (runningMonitor) {
            runningMonitor.notifyAll();
        }

        // wait for finish permit
        synchronized (endingMonitor) {
            // just finish
        }
    }
}
