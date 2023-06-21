package com.myorg;

import software.amazon.awscdk.Stage;
import software.amazon.awscdk.StageProps;
import software.constructs.Construct;

/**
 * The Pipeline stage that deploys Elastic Beanstalk stacks.
 */
public class CdkEBStage extends Stage {

    public CdkEBStage(final Construct scope, final String id, final StageProps stageProps) {
        super(scope, id, stageProps);

        new EBWebAppStack(this, "EBWebAppService", null);
    }

}
