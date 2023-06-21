package com.myorg;

import software.amazon.awscdk.SecretValue;
import software.amazon.awscdk.pipelines.*;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

import java.util.Collections;

public class CodePipelineStack extends Stack {

    private static final String PIPELINE_NAME = "WebAppPipeline";
    private static final String GITHUB_TOKEN = "github-oauth-token";
    private static final String GITHUB_REPO = "jtan22/cdk-pipeline-java-demo";
    private static final String GITHUB_BRANCH = "main";
    private static final String INSTALL_COMMAND = "npm i -g npm@latest";
    private static final String SYNTH_COMMAND = "npx cdk synth";
    private static final String SYNTH_ID = "EBWebAppSynthesizer";
    private static final String PIPELINE_STAGE_ID = "Pre-Prod";

    /**
     * Build a Pipeline with 3 initial stages: Source(input), Build(installCommands), UpdatePipeline(commands).
     * Then add our own stages: PublishAssets(Zip file of the webapp) and Stage1.
     *
     * @param scope
     * @param id
     * @param props
     */
    public CodePipelineStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // The code that defines your stack goes here

        GitHubSourceOptions sourceOptions = GitHubSourceOptions.builder().
                authentication(SecretValue.secretsManager(GITHUB_TOKEN)).build();
        ShellStepProps shellStepProps = ShellStepProps.builder().
                input(CodePipelineSource.gitHub(GITHUB_REPO, GITHUB_BRANCH, sourceOptions)).
                installCommands(Collections.singletonList(INSTALL_COMMAND)).
                commands(Collections.singletonList(SYNTH_COMMAND)).build();
        CodePipeline pipeline = CodePipeline.Builder.create(this, PIPELINE_NAME).
                pipelineName(PIPELINE_NAME).
                synth(new ShellStep(SYNTH_ID, shellStepProps)).build();
        pipeline.addStage(new CdkEBStage(this, PIPELINE_STAGE_ID, null));
    }
}
