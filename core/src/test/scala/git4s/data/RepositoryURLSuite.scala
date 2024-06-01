package git4s.data

import git4s.data.RepositoryURL

class RepositoryURLSuite extends munit.FunSuite:

  test("Parse RepositoryURL with github provider with HTTPS") {
    assert(RepositoryURL.fromString("https://github.com/geirolz/git4s.git").isDefined)
    assert(RepositoryURL.fromString("https://username:password@github.com/geirolz/git4s.git").isDefined)
    assert(RepositoryURL.fromString("git@github.com:geirolz/git4s.git").isDefined)
  }

  // builder
  test("Build RepositoryURL with github provider with HTTPS") {

    val result: RepositoryURL =
      RepositoryURL.github
        .repository("geirolz", "git4s")
        .https
        .unsafe

    assertEquals(result.value, "https://github.com/geirolz/git4s.git")
  }

  test("Build RepositoryURL with github provider with HTTPS authenticated") {

    val result: RepositoryURL =
      RepositoryURL.github
        .repository("geirolz", "git4s")
        .httpsAuth("username", "password")
        .unsafe

    assertEquals(result.value, "https://username:password@github.com/geirolz/git4s.git")
  }

  test("Build RepositoryURL with github provider with SSH") {

    val result: RepositoryURL =
      RepositoryURL.github
        .repository("geirolz", "git4s")
        .ssh
        .unsafe

    assertEquals(result.value, "git@github.com:geirolz/git4s.git")
  }
