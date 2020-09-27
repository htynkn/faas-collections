package main

import (
	"context"
	"encoding/json"
	"fmt"
	"os"

	gr "github.com/awesome-fc/golang-runtime"
	"github.com/google/go-github/v32/github"
	"golang.org/x/oauth2"
)

func handler(ctx *gr.FCContext, event []byte) ([]byte, error) {
	fcLogger := gr.GetLogger().WithField("requestId", ctx.RequestID)
	_, err := json.Marshal(ctx)
	if err != nil {
		fcLogger.Error("error:", err)
	}

	githubToken, _ := os.LookupEnv("GITHUB_TOKEN")
	fcLogger.Infof(githubToken)

	ts := oauth2.StaticTokenSource(
		&oauth2.Token{AccessToken: githubToken},
	)
	context := context.Background()
	tc := oauth2.NewClient(context, ts)
	client := github.NewClient(tc)

	var repos = [...]string{"spring-boot", "dubbo",
		"dubbo-admin", "htynkn/nacos",
		"spring-framework", "spring-boot",
	}

	for _, value := range repos {
		repo, _, err := client.Repositories.Get(context, "htynkn", value)

		if err != nil {
			fcLogger.Errorf("fail to call github api")
		}

		if repo.GetParent() != nil {
			fcLogger.Infof("find parent for %s as %s", repo.GetName(), repo.GetParent().GetFullName())

			prepo, _, err := client.Repositories.GetBranch(context, repo.GetParent().GetOwner().GetLogin(), repo.GetParent().GetName(), repo.GetDefaultBranch())

			if err != nil {
				fcLogger.Errorf("fail to call github api")
			}

			targetSha := prepo.GetCommit().GetSHA()

			fcLogger.Infof("find target sha for %s as %s", repo.GetFullName(), targetSha)

			ref := fmt.Sprintf("refs/heads/%s", repo.GetDefaultBranch())

			client.Git.UpdateRef(context, "htynkn", repo.GetName(), &github.Reference{
				Ref: &ref,
				Object: &github.GitObject{
					SHA: &targetSha,
				},
			}, true)

			fcLogger.Infof("update ref success for %s with %s", repo.GetFullName(), targetSha)
		} else {
			fcLogger.Infof("skip repo %s because of no parent repo", repo.GetFullName())
		}
	}

	fcLogger.Infof("client is ready")
	return event, nil
}

func main() {
	gr.Start(handler, nil)
}
