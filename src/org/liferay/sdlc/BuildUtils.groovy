package org.liferay.sdlc;

import jenkins.model.Jenkins;

static def lastBuildNumber(jobName) {
    return Jenkins.instance.getItem(jobName).lastSuccessfulBuild.number
}
