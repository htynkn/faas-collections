use octocrab::models::repos::Object;
use octocrab::models::Repository;
use octocrab::Octocrab;
use octocrab::params::repos::Reference;
use serde_json::Value;
use warp::{Rejection, Reply};
use warp::http::StatusCode;

pub async fn run() -> Result<impl Reply, Rejection> {
    let token = read_env_var("GITHUB_TOKEN");

    let octocrab = Octocrab::builder().personal_token(token).build().unwrap();

    let repo_list = vec!["Jackett"];

    for repo_name in repo_list {
        let repo = octocrab.repos("htynkn", repo_name).get().await.unwrap();

        match repo.parent {
            None => {
                info!("current repo:{} not have parent info, skip", repo_name)
            }
            Some(parent_repo) => {
                match parent_repo.full_name {
                    None => {
                        info!("current repo:{} not have full name, skip", repo_name)
                    }
                    Some(full_name) => {
                        let parent_repo: Repository = octocrab.get(format!("/repos/{}", full_name), None::<&()>).await.unwrap();
                        info!("find parent:{} with branch:{:?}",full_name,parent_repo.default_branch);

                        let default_branch = parent_repo.default_branch.unwrap();
                        let parent_ref = octocrab.repos(
                            parent_repo.owner.unwrap().login, parent_repo.name,
                        ).get_ref(&Reference::Branch(default_branch.to_string())).await.unwrap();


                        let sha = if let Object::Commit { sha, .. } = parent_ref.object {
                            sha
                        } else {
                            panic!()
                        };

                        info!("find ref {:?} to repo:{}", sha.to_string(), full_name);

                        let x: Value = octocrab.patch(format!("/repos/htynkn/{}/git/refs/heads/{}", repo_name, &default_branch), Some(&serde_json::json!({ "sha": sha.to_string() }))).await.unwrap();

                        info!("update success for {} with {:?}", full_name,x);
                    }
                }
            }
        }
    }

    Ok(StatusCode::OK)
}

fn read_env_var(var_name: &str) -> String {
    let err = format!("Missing environment variable: {}", var_name);
    std::env::var(var_name).expect(&err)
}
