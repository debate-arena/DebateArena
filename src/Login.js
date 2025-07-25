import React from "react";

const GOOGLE_AUTH_URL = "http://localhost:8080/oauth2/authorization/google";

function Login() {
  return (
    <div>
      <h2>구글 소셜 로그인 테스트</h2>
      <a href={GOOGLE_AUTH_URL}>
        <button>구글로 로그인</button>
      </a>
    </div>
  );
}

export default Login;