package git4s.data

import git4s.data.GitRepositoryURL

class GitRepositoryURLSuite extends munit.FunSuite:

  test("Parse RepositoryURL with github provider with HTTPS") {
    assert(GitRepositoryURL.fromString("https://github.com/geirolz/git4s.git").isDefined)
    assert(GitRepositoryURL.fromString("https://username:password@github.com/geirolz/git4s.git").isDefined)
    assert(GitRepositoryURL.fromString("git@github.com:geirolz/git4s.git").isDefined)
  }

  // builder
  test("Build RepositoryURL with github provider with HTTPS") {

    val result: GitRepositoryURL =
      GitRepositoryURL.github
        .repository("geirolz", "git4s")
        .https
        .unsafe

    assertEquals(result.value, "https://github.com/geirolz/git4s.git")
  }

  test("Build RepositoryURL with github provider with HTTPS authenticated") {

    val result: GitRepositoryURL =
      GitRepositoryURL.github
        .repository("geirolz", "git4s")
        .httpsAuth("username", "password")
        .unsafe

    assertEquals(result.value, "https://username:password@github.com/geirolz/git4s.git")
  }

  test("Build RepositoryURL with github provider with SSH") {

    val result: GitRepositoryURL =
      GitRepositoryURL.github
        .repository("geirolz", "git4s")
        .ssh
        .unsafe

    assertEquals(result.value, "git@github.com:geirolz/git4s.git")
  }
