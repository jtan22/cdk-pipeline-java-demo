package com.myorg;

import software.amazon.awscdk.pipelines.CodePipeline;
import software.amazon.awscdk.pipelines.CodePipelineSource;
import software.amazon.awscdk.pipelines.ShellStep;
import software.amazon.awscdk.pipelines.ShellStepProps;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

import java.util.Collections;

public class CodePipelineStack extends Stack {

    public CodePipelineStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public CodePipelineStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // The code that defines your stack goes here

        CodePipeline pipeline = CodePipeline.Builder.
                create(this, "WebAppPipeline").
                pipelineName("WebAppPipeline").
                synth(new ShellStep("Synth",
                        ShellStepProps.builder().
                                input(CodePipelineSource.gitHub("jtan22/cdk-pipeline-java-demo", "main")).
                                installCommands(Collections.singletonList("npm i -g npm@latest")).
                                commands(Collections.singletonList("npx cdk synth")).
                                build())).
                build();
        CdkEBStage stage = new CdkEBStage(this, "Pre-Prod", null);
        pipeline.addStage(stage);
    }
}
