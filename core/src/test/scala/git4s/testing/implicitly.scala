package git4s.testing

extension [A](a: A) def useImplicitly[B](f: A ?=> B): B = f(using a)
