package com.codesentinel.review.ai;

import com.codesentinel.review.dto.AIReviewOutput;
import com.codesentinel.review.dto.ReviewFinding;
import com.codesentinel.review.dto.ReviewFileInput;
import com.codesentinel.review.dto.ReviewRequest;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponseCreateParams;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OpenAIProvider implements AIProvider {

    private final OpenAIClient openAIClient;

    public OpenAIProvider(OpenAIClient openAIClient) {
        this.openAIClient = openAIClient;
    }

    @Override
    public List<ReviewFinding> analyze(ReviewRequest request) {

        String prompt = buildReviewPrompt(request);

        StructuredResponseCreateParams<AIReviewOutput> params =
                ResponseCreateParams.builder()
                        .input(prompt)
                        .model("gpt-5")
                        .text(AIReviewOutput.class)
                        .build();

        AIReviewOutput output =
                openAIClient
                        .responses()
                        .create(params)
                        .output()
                        .stream()
                        .flatMap(item -> item.message().stream())
                        .flatMap(message -> message.content().stream())
                        .flatMap(content -> content.outputText().stream())
                        .findFirst()
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "OpenAI returned no structured review output"
                                )
                        );

        return mapFindings(output);
    }

    private String buildReviewPrompt(ReviewRequest request) {

        StringBuilder prompt = new StringBuilder();

        prompt.append("""
                You are CodeSentinel AI, an expert software code reviewer.

                Review the provided GitHub Pull Request changes.

                Analyze the changed code for:

                1. Bugs and reliability problems
                2. Security vulnerabilities
                3. Performance issues
                4. Code quality problems
                5. Maintainability issues

                Only report issues that are supported by the provided code.
                Do not invent problems.

                Severity must be one of:
                LOW, MEDIUM, HIGH, CRITICAL

                Category should describe the type of issue, for example:
                BUG, SECURITY, PERFORMANCE, CODE_QUALITY, MAINTAINABILITY

                If an issue can be associated with a specific line in the patch,
                provide the line number. Otherwise use null.

                Keep findings concise and actionable.

                Pull Request:
                """);

        prompt.append("\nNumber: ")
                .append(request.pullRequest().number());

        prompt.append("\nTitle: ")
                .append(request.pullRequest().title());

        prompt.append("\nDescription: ")
                .append(
                        request.pullRequest().description() == null
                                ? ""
                                : request.pullRequest().description()
                );

        prompt.append("\nBase branch: ")
                .append(request.pullRequest().baseBranch());

        prompt.append("\nHead branch: ")
                .append(request.pullRequest().headBranch());

        prompt.append("\n\nChanged files:\n");

        for (ReviewFileInput file : request.files()) {

            prompt.append("\n--- FILE ---\n");

            prompt.append("Filename: ")
                    .append(file.filename())
                    .append("\n");

            prompt.append("Status: ")
                    .append(file.status())
                    .append("\n");

            prompt.append("Additions: ")
                    .append(file.additions())
                    .append("\n");

            prompt.append("Deletions: ")
                    .append(file.deletions())
                    .append("\n");

            prompt.append("Changes: ")
                    .append(file.changes())
                    .append("\n");

            prompt.append("Patch:\n")
                    .append(
                            file.patch() == null
                                    ? ""
                                    : file.patch()
                    );

            prompt.append("\n--- END FILE ---\n");
        }

        return prompt.toString();
    }

    private List<ReviewFinding> mapFindings(AIReviewOutput output) {

        if (output == null || output.findings == null) {
            return List.of();
        }

        List<ReviewFinding> findings = new ArrayList<>();

        for (AIReviewOutput.AIReviewFinding finding : output.findings) {

            findings.add(
                    new ReviewFinding(
                            finding.severity,
                            finding.category,
                            finding.filename,
                            finding.line,
                            finding.title,
                            finding.description,
                            finding.suggestion
                    )
            );
        }

        return findings;
    }
}