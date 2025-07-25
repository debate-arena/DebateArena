import React from "react";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import Login from "./Login";
import OAuth2RedirectHandler from "./OAuth2RedirectHandler";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/oauth2/redirect" element={<OAuth2RedirectHandler />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
