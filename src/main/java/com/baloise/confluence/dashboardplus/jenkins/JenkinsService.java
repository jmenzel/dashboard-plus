package com.baloise.confluence.dashboardplus.jenkins;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.baloise.confluence.dashboardplus.exception.ResourceNotFoundException;
import com.baloise.confluence.dashboardplus.exception.ServiceUnavailableException;
import com.baloise.confluence.dashboardplus.jenkins.bean.JenkinsData;
import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.JobWithDetails;
import com.offbytwo.jenkins.model.TestReport;

public class JenkinsService implements IJenkinsService {

	public static JenkinsData createServiceAndFetchData(String host,
			String jobName, boolean recursiveChildLoading)
			throws ServiceUnavailableException, ResourceNotFoundException {
		return new JenkinsService().fetchData(host, jobName,
				recursiveChildLoading);
	}

	@Override
	public JenkinsData fetchData(String host, String jobName,
			boolean recursiveChildLoading) throws ServiceUnavailableException,
			ResourceNotFoundException {
		JenkinsData result;
		try {
			JenkinsServer jenkins = new JenkinsServer(new URI(host));
			JobWithDetails job = jenkins.getJob(jobName);

			if (job == null) {
				throw new ResourceNotFoundException("No job '" + jobName
						+ "' found on Jenkins @ " + host);
			}

			result = new JenkinsData();
			result.setJobDetails(job.details());

			Build lastCompletedBuild = null;
			try {
				lastCompletedBuild = job.getLastCompletedBuild();
			} catch (NullPointerException e) {
				// Possibly thrown by job.getLastCompletedBuild() when no completed build available
			}
			if (lastCompletedBuild != null) {
				result.setLastCompletedBuildDetails(lastCompletedBuild
						.details());
				TestReport testReport;
				try {
					testReport = lastCompletedBuild
							.testReport(recursiveChildLoading);
				} catch (IOException e) {
					testReport = new TestReport();
				}
				result.setLastCompletedBuildTestReport(testReport);
			}

		} catch (IOException e) {
			throw new ServiceUnavailableException(
					"IO exception occured while retrieving information from Jenkins @ "
							+ host + ", root cause: " + e.getMessage(), e);
		} catch (URISyntaxException e) {
			throw new ServiceUnavailableException(
					"Unexpected exception occured while retrieving information from Jenkins @ "
							+ host + ", root cause: " + e.getMessage(), e);
		} catch (RuntimeException e) {
			throw new ServiceUnavailableException(
					"Runtime exception occured while retrieving information from Jenkins @ "
							+ host + ", root cause: " + e.getMessage(), e);
		}
		return result;
	}

}
