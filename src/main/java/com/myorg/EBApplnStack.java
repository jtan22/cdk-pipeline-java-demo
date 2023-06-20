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
import java.util.List;

public class EBApplnStack extends Stack {

    public EBApplnStack(final Construct scope, final String id, final StackProps stackProps) {
        super(scope, id, stackProps);

        Asset webAppZipArchive = Asset.Builder.
                create(this, "WebAppZip").
                path("${__dirname}/../webapp").
                build();
        String appName = "MyWebApp";
        CfnApplication app = CfnApplication.Builder.
                create(this, "Application").
                applicationName(appName).
                build();
        CfnApplicationVersion appVersion = CfnApplicationVersion.Builder.
                create(this, "AppVersion").
                applicationName(appName).
                sourceBundle(CfnApplicationVersion.SourceBundleProperty.builder().
                        s3Bucket(webAppZipArchive.getS3BucketName()).
                        s3Key(webAppZipArchive.getS3ObjectKey()).
                        build()).
                build();
        appVersion.addDependency(app);
        Role myRole = Role.Builder.
                create(this, "MyWebApp-aws-elasticbeanstalk-ec2-role").
                assumedBy(ServicePrincipal.Builder.create("ec2.amazonaws.com").build()).
                build();
        myRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("AWSElasticBeanstalkWebTier"));
        String myProfileName = "MyWebApp-InstanceProfile";
        CfnInstanceProfile instanceProfile = CfnInstanceProfile.Builder.
                create(this, myProfileName).
                instanceProfileName(myProfileName).roles(Collections.singletonList(myRole.getRoleName())).
                build();
        List<CfnEnvironment.OptionSettingProperty> optionSettingProperties = Arrays.asList(
                CfnEnvironment.OptionSettingProperty.builder().
                        namespace("aws:autoscaling:launchconfiguration").
                        optionName("IamInstanceProfile").
                        value(myProfileName).
                        build(),
                CfnEnvironment.OptionSettingProperty.builder().
                        namespace("aws:autoscaling:asg").
                        optionName("MinSize").
                        value("1").
                        build(),
                CfnEnvironment.OptionSettingProperty.builder().
                        namespace("aws:autoscaling:asg").
                        optionName("MaxSize").
                        value("2").
                        build(),
                CfnEnvironment.OptionSettingProperty.builder().
                        namespace("aws:ec2:instances").
                        optionName("InstanceTypes").
                        value("t2.micro").
                        build()
        );
        CfnEnvironment elbEnv = CfnEnvironment.Builder.
                create(this, "Environment").
                environmentName("MyWebAppEnvironment").
                applicationName(appName).
                solutionStackName("64bit Amazon Linux 2 v5.8.2 running Node.js 18").
                optionSettings(optionSettingProperties).
                versionLabel(appVersion.getRef()).
                build();
    }
}
