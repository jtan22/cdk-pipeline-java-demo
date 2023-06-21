package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.elasticbeanstalk.CfnApplication;
import software.amazon.awscdk.services.elasticbeanstalk.CfnApplicationVersion;
import software.amazon.awscdk.services.elasticbeanstalk.CfnEnvironment;
import software.amazon.awscdk.services.iam.CfnInstanceProfile;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.s3.assets.Asset;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.Collections;

/**
 * Create Beanstalk stack, it's application and environment.
 */
public class EBWebAppStack extends Stack {

    private static final String APP_NAME = "EBWebApp";
    private static final String APP_VERSION_ID = "EBWebApp-Version";
    private static final String PROFILE_NAME = "EBWebApp-InstanceProfile";
    private static final String ZIP_ID = "EBWebAppZip";
    private static final String WEB_APP_PATH = "${__dirname}/../webapp";
    private static final String EC2_ROLE_NAME = "EBWebApp-aws-elasticbeanstalk-ec2-role";
    private static final String EC2_SERVICE_NAME = "ec2.amazonaws.com";
    private static final String EC2_POLICY_NAME = "AWSElasticBeanstalkWebTier";
    private static final String ENVIRONMENT_NAME = "EBWebApp-Environment";
    private static final String SOLUTION_STACK_NAME = "64bit Amazon Linux 2 v5.8.2 running Node.js 18";

    public EBWebAppStack(final Construct scope, final String id, final StackProps stackProps) {
        super(scope, id, stackProps);

        CfnApplicationVersion appVersion = createAppVersion();
        appVersion.addDependency(createApp());
        createProfile();
        createEnvironment(appVersion.getRef());
    }

    private CfnApplication createApp() {
        return CfnApplication.Builder.create(this, APP_NAME).applicationName(APP_NAME).build();
    }

    private CfnApplicationVersion createAppVersion() {
        Asset webAppZip = Asset.Builder.create(this, ZIP_ID).
                path(WEB_APP_PATH).build();
        CfnApplicationVersion.SourceBundleProperty sourceBundle = CfnApplicationVersion.SourceBundleProperty.builder().
                s3Bucket(webAppZip.getS3BucketName()).
                s3Key(webAppZip.getS3ObjectKey()).
                build();
        return CfnApplicationVersion.Builder.create(this, APP_VERSION_ID).
                applicationName(APP_NAME).
                sourceBundle(sourceBundle).build();
    }

    private void createProfile() {
        Role role = Role.Builder.create(this, EC2_ROLE_NAME).
                assumedBy(ServicePrincipal.Builder.create(EC2_SERVICE_NAME).build()).build();
        role.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName(EC2_POLICY_NAME));
        CfnInstanceProfile.Builder.create(this, PROFILE_NAME).
                instanceProfileName(PROFILE_NAME).
                roles(Collections.singletonList(role.getRoleName())).build();
    }

    /**
     * Create Environment.
     *
     * Run this aws command to get a list of solution stack names:
     *      aws elasticbeanstalk list-available-solution-stacks
     *
     * @param versionLabel
     */
    private void createEnvironment(String versionLabel) {
        CfnEnvironment.Builder.create(this, ENVIRONMENT_NAME).
                environmentName(ENVIRONMENT_NAME).
                applicationName(APP_NAME).
                solutionStackName(SOLUTION_STACK_NAME).
                optionSettings(Arrays.asList(
                        createOptionSettingProfile(),
                        createOptionSettingMinSize(),
                        createOptionSettingMaxSize(),
                        createOptionSettingInstanceType())).
                versionLabel(versionLabel).
                build();
    }

    private CfnEnvironment.OptionSettingProperty createOptionSettingProfile() {
        return CfnEnvironment.OptionSettingProperty.builder().
                namespace("aws:autoscaling:launchconfiguration").optionName("IamInstanceProfile").value(PROFILE_NAME).build();
    }

    private CfnEnvironment.OptionSettingProperty createOptionSettingMinSize() {
        return CfnEnvironment.OptionSettingProperty.builder().
                namespace("aws:autoscaling:asg").optionName("MinSize").value("1").build();
    }

    private CfnEnvironment.OptionSettingProperty createOptionSettingMaxSize() {
        return CfnEnvironment.OptionSettingProperty.builder().
                namespace("aws:autoscaling:asg").optionName("MaxSize").value("2").build();
    }

    private CfnEnvironment.OptionSettingProperty createOptionSettingInstanceType() {
        return CfnEnvironment.OptionSettingProperty.builder().
                namespace("aws:ec2:instances").optionName("InstanceTypes").value("t2.micro").build();
    }
}