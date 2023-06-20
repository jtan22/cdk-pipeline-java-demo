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

public class EBWebAppStack extends Stack {

    private static final String APP_NAME = "EBWebApp";
    private static final String PROFILE_NAME = "EBWebApp-InstanceProfile";

    public EBWebAppStack(final Construct scope, final String id, final StackProps stackProps) {
        super(scope, id, stackProps);

        CfnApplicationVersion appVersion = createAppVersion();
        appVersion.addDependency(createApp());
        createProfile();
        createEnvironment(appVersion);
    }

    private CfnApplication createApp() {
        return CfnApplication.Builder.create(this, "EBApplication").applicationName(APP_NAME).build();
    }

    private CfnApplicationVersion createAppVersion() {
        Asset webAppZipArchive = Asset.Builder.create(this, "EBWebAppZip").
                path("${__dirname}/../webapp").build();
        CfnApplicationVersion.SourceBundleProperty sourceBundle = CfnApplicationVersion.SourceBundleProperty.builder().
                s3Bucket(webAppZipArchive.getS3BucketName()).
                s3Key(webAppZipArchive.getS3ObjectKey()).
                build();
        return CfnApplicationVersion.Builder.create(this, "EBWebAppVersion").
                applicationName(APP_NAME).
                sourceBundle(sourceBundle).build();
    }

    private void createProfile() {
        Role role = Role.Builder.create(this, "EBWebApp-aws-elasticbeanstalk-ec2-role").
                assumedBy(ServicePrincipal.Builder.create("ec2.amazonaws.com").build()).build();
        role.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("AWSElasticBeanstalkWebTier"));
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
     * @param appVersion
     */
    private void createEnvironment(CfnApplicationVersion appVersion) {
        CfnEnvironment.Builder.create(this, "EBWebAppEnvironment").
                environmentName("EBWebAppEnvironment").
                applicationName(APP_NAME).
                solutionStackName("64bit Amazon Linux 2 v5.8.2 running Node.js 18").
                optionSettings(Arrays.asList(
                        createOptionSettingProfile(),
                        createOptionSettingMinSize(),
                        createOptionSettingMaxSize(),
                        createOptionSettingInstanceType())).
                versionLabel(appVersion.getRef()).
                build();
    }

    private CfnEnvironment.OptionSettingProperty createOptionSettingProfile() {
        return CfnEnvironment.OptionSettingProperty.builder().
                namespace("aws:autoscaling:launchconfiguration").
                optionName("IamInstanceProfile").
                value(PROFILE_NAME).
                build();
    }

    private CfnEnvironment.OptionSettingProperty createOptionSettingMinSize() {
        return CfnEnvironment.OptionSettingProperty.builder().
                namespace("aws:autoscaling:asg").
                optionName("MinSize").
                value("1").
                build();
    }

    private CfnEnvironment.OptionSettingProperty createOptionSettingMaxSize() {
        return CfnEnvironment.OptionSettingProperty.builder().
                namespace("aws:autoscaling:asg").
                optionName("MaxSize").
                value("2").
                build();
    }

    private CfnEnvironment.OptionSettingProperty createOptionSettingInstanceType() {
        return CfnEnvironment.OptionSettingProperty.builder().
                namespace("aws:ec2:instances").
                optionName("InstanceTypes").
                value("t2.micro").
                build();
    }
}