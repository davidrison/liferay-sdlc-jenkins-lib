package org.liferay.sdlc;

import java.io.File;
import java.net.URL;
import java.util.Base64;
import jenkins.model.*;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;
import org.jenkinsci.remoting.RoleChecker;
import java.util.Base64;

def createFilePath(path) {
    if (!path.startsWith("/"))
        path = "$workspace/$path";
    if ("master".equals(env.NODE_NAME))
        return new FilePath(new File(path));

    def channel = Jenkins.instance.getComputer(env.NODE_NAME).channel;
    return new FilePath(channel, path);
}

def downloadTo(String urlFrom, toPath) {
    println "downloadTo $urlFrom -> $toPath"
    FilePath fp = createFilePath(toPath);
    fp.copyFrom(new URL(urlFrom));
    return fp;
}

def downloadToWithAuth(String urlFrom, toPath, user, password) {
    println "downloadTo $urlFrom -> $toPath"
    FilePath fp = createFilePath(toPath);
    def conn = new URL(urlFrom).openConnection();
    def auth = Base64.getEncoder().encodeToString("${user}:${password}".bytes);
    conn.addRequestProperty("Authorization", "Basic " + auth);
    fp.copyFrom(conn.inputStream);
    return fp;
}


def mkdir(path) {
    println "mkdir $path"
    createFilePath(path).mkdirs();
}

def remove(path) {
    println "remove $path"
    if (fileExists(path))
        createFilePath(path).delete();
}

def removeRecursively(path) {
    println "remove $path"
    if (fileExists(path))
        createFilePath(path).deleteRecursive();
}


def copyRecursive(from, to) {
    println "copyRecursiveTo $from -> $to"
    createFilePath(from).copyRecursiveTo(createFilePath(to));
}

def renameTo(from, to) {
    println "renameTo $from to $to"
    createFilePath(from).renameTo(createFilePath(to));
}
