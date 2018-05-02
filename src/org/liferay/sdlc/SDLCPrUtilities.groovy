package org.liferay.sdlc;

import groovy.json.JsonSlurper

import java.util.List;
import java.util.Map;

import hudson.slaves.EnvironmentVariablesNodeProperty;
import jenkins.model.Jenkins;
import org.liferay.sdlc.Utilities;

class SDLCPrUtilities {

    static def failureReasonFileName = "failureReasonFile";

    static def hasToClosePullRequestOnFailure = false;

    static def _ = new Utilities();

    // This method can't be "NonCPS"
    static def prInit(projectKey, projectName) {
        def settingsGradle = _._readFile("settings.gradle");
        if (!settingsGradle.contains("rootProject.name")) {
            settingsGradle+="\nrootProject.name=\"$projectKey\"";
            _._writeFile("settings.gradle", settingsGradle);
        }

        if (!_._fileExists('build.gradle')) {
            log 'file build.gradle not found'
            return;
        }

        def buildGradle = _._readFile('build.gradle');
        buildGradle += '\n\n' + _.loadLibrary('org/liferay/sdlc/failure-reason-report.gradle');

        if (!buildGradle.contains('sonarqube')) {
            def varsToReplace = [
                "_SONAR_PROJECT_NAME_" : projectName,
                "_SONAR_PROJECT_KEY_"  : projectKey
            ];

            buildGradle += '\n\n' + replaceVars(_.loadLibrary('org/liferay/sdlc/sonarqube-task.gradle'), varsToReplace);
        }

         _._writeFile('build.gradle', buildGradle);
    }

    @NonCPS
    static def getLogin(String json) {
        return new JsonSlurper().parseText(json).user.login
    }

    @NonCPS
    static def getEmail(String json) {
        return new JsonSlurper().parseText(json).email
    }

    @NonCPS
    static def isPullRequest() {
        return _.ChangeId() != null
    }

    static def closePullRequestOnFailure() {
        hasToClosePullRequestOnFailure = true;
    }

    static def shouldClosePullRequest() {

        if (!hasToClosePullRequestOnFailure) {
            log "hasToClosePullRequestOnFailure: false"
            return false;
        }

        if (!_._fileExists(failureReasonFileName)) {
            log "Error is not considered to be introduced by new code"
            return false;
        }

        def reasonText = _._readFile(failureReasonFileName).trim();

        if (reasonText.matches(".*org.gradle.api.internal.tasks.compile.CompilationFailedException.*"))
            return true;

        if (reasonText.matches(".*There were failing tests.*"))
            return true;

        if (reasonText.matches(".*Task: sonarqube.*New Blocker.*"))
            return true;

        return false;
    }

    static def handleError(gitRepository, emailLeader, gitAuthentication) {
        if (!isPullRequest())
            return;

        if (!shouldClosePullRequest()) {
            log "Will not close PR"
            return;
        }

        log "Will close PR"
        _.closePullRequest(gitRepository, emailLeader, gitAuthentication);
    }

    // This method can't be "NonCPS"
    static def appendAdditionalCommand(fileName, varMap) {
        def additionalCustomCommands = replaceVars(_.loadLibrary("org/liferay/sdlc/custom.gradle"), varMap);

        if (!_._fileExists(fileName)) {
            log "file $fileName not found"
            return;
        }    

        def contents = _._readFile(fileName);
        contents += '\n\n'+ additionalCustomCommands;
           
        _._writeFile(fileName, contents);
    }

    static def replaceVars(String value, Map<String, String> vars) {
        for (var in vars) 
            value = value.replace('#{' + var.key + '}', var.value);

        return value;
    }

    static def sonarqube(gitRepository) {

        if (!_.isSonarVerificationEnabled()) {
            log "Sonar verification is disabled."
            return;
        }

        def command = [
            "sonarqube",
            "-Dsonar.buildbreaker.queryMaxAttempts=90",
            "-Dsonar.buildbreaker.skip=true",
            "-Dsonar.host.url=${_.SonarHostUrl()}",
            '-Dsonar.issuesReport.console.enable=true',
            '-Dsonar.issuesReport.html.enable=true',
            '-Dsonar.login="$SONAR_USER"',
            '-Dsonar.password="$SONAR_PASSWORD"'
        ]

        if (isPullRequest()) {
            println "Sonarqube Pull Request Evaluation"

            command += "-Dsonar.analysis.mode=preview"
            command += "-Dsonar.github.pullRequest=${_.ChangeId()}"
            command += "-Dsonar.github.oauth=${_.GithubOauth()}"
            command += "-Dsonar.github.repository=${gitRepository}"
        } else {
            command += "-Dsonar.analysis.mode=publish"
        }

        _._withCredentials("sonar_analyser", "SONAR_USER", "SONAR_PASSWORD") {
            try {
                gradlew command.join(' ')
                if (isPullRequest()) {
                    sonarResultCheck();
                }
            } catch(Exception e) {
                if (!_._fileExists(failureReasonFileName)) throw e;
                def reasonText = _._readFile(failureReasonFileName).trim();
                if (reasonText.matches(".*sonarqube:.*Unable to perform GitHub WS operation:")) {
                    throw new Exception("Sonarqube Failed to access github repository to write sonarqube analysis.");
                }
            }
        }
    }

    static def sonarResultCheck() {
        def sonarReportFile = "build/sonar/issues-report/issues-report-light.html";
        if (!_._fileExists(sonarReportFile))
            return

        def blockerObj = _._getJsonFromStringUsingPattern(_._readFile(sonarReportFile), /\{[^}]+.?key.?:\s*.?critical.?[^}]+}/)
        if (blockerObj == null || blockerObj.newtotal <= 0)
            return

        def failureReason = ""
        if (_._fileExists(failureReasonFileName))
            failureReason = _._readFile(failureReasonFileName) + System.getProperty("line.separator")

        def plural = blockerObj.newtotal > 1 ? "s" : ""
        failureReason += "Task: sonarqube Reason: "+ blockerObj.newtotal +" blocker"+plural+" Message: New Blocker Issue"+plural+" found"

        _._writeFile(failureReasonFileName, failureReason)
    }

    static def gradlew(args) {
        _._withCredentials("nexusCredentials", "NEXUS_USER", "NEXUS_PASSWORD") {
            _._gradlew(args)
        }
    }

    @NonCPS
    static def log(args) {
        _.log(args)
    }
}
