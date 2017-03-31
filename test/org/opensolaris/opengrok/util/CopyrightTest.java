/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

 /*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 */
package org.opensolaris.opengrok.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.opensolaris.opengrok.condition.ConditionalRun;
import org.opensolaris.opengrok.condition.RepositoryInstalled;
import org.opensolaris.opengrok.history.GitRepository;

/**
 *
 * @author Krystof Tulinger
 */
@ConditionalRun(condition = RepositoryInstalled.GitInstalled.class)
@RunWith(Parameterized.class)
public class CopyrightTest {

    private static final String REMOTE_REPOSITORY = "https://github.com/OpenGrok/OpenGrok";
    private static final String REMOTE_BRANCH = "master";
    private static final String REMOTE_ALIAS = "upstream-" + System.currentTimeMillis();
    private static final String LOCAL_ALIAS = "origin";
    private static boolean working = true;

    private static File repositoryDirectory = null;
    private final File file;

    @Parameters
    public static Collection<File[]> data() throws IOException {
        repositoryDirectory = locateGitRepository();
        working = true;

        Set<File> files = new TreeSet<>();
        files.addAll(getStaged());
        files.addAll(getCommited());
        files.addAll(getUnstaged());
        files.addAll(getUntracked());
        return files.stream().map(new Function<File, File[]>() {
            @Override
            public File[] apply(File t) {
                return new File[]{t};
            }
        }).collect(Collectors.toSet());
    }

    @Before
    public void setUp() {
        Assume.assumeTrue(working);
    }

    public CopyrightTest(File file) {
        this.file = file;
    }

    protected static String ensureCommand(String propertyKey, String fallbackCommand) {
        return System.getProperty(propertyKey, fallbackCommand);
    }

    public static File locateGitRepository() throws IOException {
        File repoDirectory = null;
        File relFile = null;

        try {
            relFile = new File(CopyrightTest.class.getProtectionDomain().getCodeSource().getLocation().getFile());
        } catch (SecurityException | NullPointerException ex) {
            throw new IOException("Unable to determine current file location.", ex);
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(ensureCommand(GitRepository.CMD_PROPERTY_KEY, GitRepository.CMD_FALLBACK));
        cmd.add("rev-parse");
        cmd.add("--show-toplevel");

        Executor executor = new Executor(cmd, relFile);
        if (executor.exec() != 0) {
            throw new IOException(
                    String.format("Unable to execute \"%s\": %s", cmd, executor.getErrorString()));
        }

        repoDirectory = new File(executor.getOutputString().trim());
        if (!repoDirectory.isDirectory() || !repoDirectory.canRead()) {
            throw new FileNotFoundException(
                    String.format("Unable to locate the base directory \"%s\".", repoDirectory.getAbsolutePath()));
        }

        cmd.clear();
        cmd.add(ensureCommand(GitRepository.CMD_PROPERTY_KEY, GitRepository.CMD_FALLBACK));
        cmd.add("config");
        cmd.add("--get");
        cmd.add("remote.origin.url");

        executor = new Executor(cmd, repoDirectory);
        if (executor.exec() != 0) {
            throw new IOException(
                    String.format("Unable to execute \"%s\": %s", cmd, executor.getErrorString()));
        }

        String name = null;
        try {
            name = new File(executor.getOutputString().trim()).getName().replaceAll("\\.git$", "");
        } catch (NullPointerException ex) {
            throw new IOException(String.format("Unable to get the output string from \"%s\".", cmd), ex);
        }
        if (!"OpenGrok".equals(name)) {
            throw new IOException(
                    String.format("This is not an OpenGrok git repository. Found name \"%s\"", executor.getOutputString()));
        }

        return repoDirectory;
    }

    @Test
    public void testCopyrights() throws IOException {
        Pattern[] patterns = new Pattern[]{
            Pattern.compile("^.*?Copyright \\(c\\) (\\d{4}),? Oracle and/or its affiliates\\. All rights reserved\\..*"),
            Pattern.compile("^.*?Copyright \\(c\\) \\d{4}, (\\d{4}),? Oracle and/or its affiliates\\. All rights reserved\\..*")
        };

        try (BufferedReader in = new BufferedReader(new FileReader(file.getCanonicalPath()))) {
            inspectFile(in, patterns);
        } catch (AssertionError e) {
            throw new AssertionError(String.format("File %s has been changed but contains an old copyright. (%s)", file, e.getLocalizedMessage()), e);
        }
    }

    protected static Set<File> getUnstaged() throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add(ensureCommand(GitRepository.CMD_PROPERTY_KEY, GitRepository.CMD_FALLBACK));
        cmd.add("diff");
        cmd.add("--name-only");

        return extractFiles(cmd, new LineHandler() {
            @Override
            public File handleLine(String line) {
                return new File(repositoryDirectory, line);
            }
        });
    }

    protected static Set<File> getUntracked() throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add(ensureCommand(GitRepository.CMD_PROPERTY_KEY, GitRepository.CMD_FALLBACK));
        cmd.add("ls-files");
        cmd.add("--others");
        cmd.add("--exclude-standard");

        return extractFiles(cmd, new LineHandler() {
            @Override
            public File handleLine(String line) {
                return new File(repositoryDirectory, line);
            }
        });
    }

    protected static Set<File> getStaged() throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add(ensureCommand(GitRepository.CMD_PROPERTY_KEY, GitRepository.CMD_FALLBACK));
        cmd.add("diff");
        cmd.add("--cached");
        cmd.add("--name-only");

        return extractFiles(cmd, new LineHandler() {
            @Override
            public File handleLine(String line) {
                return new File(repositoryDirectory, line);
            }
        });
    }

    protected static Set<File> getCommited() throws IOException {
        List<String> cmd;
        Executor executor;

        if (!(working = addUpstream())) {
            return new TreeSet<>();
        }
        working = working && trackUpstreamMaster();

        try {
            cmd = new ArrayList<>();
            cmd.add(ensureCommand(GitRepository.CMD_PROPERTY_KEY, GitRepository.CMD_FALLBACK));
            cmd.add("log");
            cmd.add("--pretty=format:commit %h");
            cmd.add("--abbrev-commit");
            cmd.add("--name-only");
            cmd.add(String.format("%s/%s..", REMOTE_ALIAS, REMOTE_BRANCH));
            return extractFiles(cmd, new LineHandler() {
                @Override
                public File handleLine(String line) {
                    if (line.startsWith("commit:")) {
                        return null;
                    }
                    File f = new File(repositoryDirectory, line);
                    if (f.exists() && f.isFile() && f.canRead()) {
                        return f;
                    }
                    return null;
                }
            });
        } finally {
            working = working && removeUpstream();
        }
    }

    private static boolean removeUpstream() {
        List<String> cmd;
        Executor executor;
        cmd = new ArrayList<>();
        cmd.add(ensureCommand(GitRepository.CMD_PROPERTY_KEY, GitRepository.CMD_FALLBACK));
        cmd.add("remote");
        cmd.add("remove");
        cmd.add(REMOTE_ALIAS);
        return new Executor(cmd, repositoryDirectory).exec() == 0;
    }

    private static boolean trackUpstreamMaster() {
        List<String> cmd;
        Executor executor;
        cmd = new ArrayList<>();
        cmd.add(ensureCommand(GitRepository.CMD_PROPERTY_KEY, GitRepository.CMD_FALLBACK));
        cmd.add("fetch");
        cmd.add("--depth=50");
        cmd.add(REMOTE_ALIAS);

        return new Executor(cmd, repositoryDirectory).exec() == 0;
    }

    private static boolean addUpstream() {
        List<String> cmd = new ArrayList<>();
        cmd.add(ensureCommand(GitRepository.CMD_PROPERTY_KEY, GitRepository.CMD_FALLBACK));
        cmd.add("remote");
        cmd.add("add");
        cmd.add(REMOTE_ALIAS);
        cmd.add(REMOTE_REPOSITORY);
        return new Executor(cmd, repositoryDirectory).exec() == 0;
    }

    protected void inspectFile(final BufferedReader in, Pattern[] patterns) throws IOException, NumberFormatException {
        String line;
        while ((line = in.readLine()) != null) {
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    Assert.assertEquals(Year.now().getValue(), Integer.parseInt(matcher.group(1)));
                    return;
                }
            }
        }
    }

    private static Set<File> extractFiles(List<String> cmd, LineHandler handler) throws IOException {
        Set<File> set = new TreeSet<>();
        Executor exec = new Executor(cmd, repositoryDirectory);

        if (exec.exec() != 0) {
            throw new IOException(
                    String.format("Git log \"%s\" should not fail.", cmd));
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(exec.getOutputStream(), "UTF-8"))) {
            String line;
            while ((line = in.readLine()) != null) {
                File f = handler.handleLine(line);
                if (f == null || !f.exists() || !f.isFile() || !f.canRead()) {
                    continue;
                }
                set.add(new File(repositoryDirectory, line));
            }
        }
        return set;
    }

    private interface LineHandler {

        public File handleLine(String line);
    }
}
