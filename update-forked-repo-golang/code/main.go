package main

import (
	"context"
	"encoding/json"
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

	ctx := context.Background()
	ts := oauth2.StaticTokenSource(
		&oauth2.Token{AccessToken: githubToken},
	)
	tc := oauth2.NewClient(ctx, ts)
	client := github.NewClient(tc)

	fcLogger.Infof("hello golang!")
	return event, nil
}

func main() {
	gr.Start(handler, nil)
}
