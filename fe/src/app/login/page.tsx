"use client";

import { login } from "@/api/User";
import CustomButton from "@/components/common/CustomBtn";
import CustomInput from "@/components/common/CustomInput";
import CustomLayout from "@/components/common/CustomLayout";
import Link from "next/link";
import { useState } from "react";

export default function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const onLogin = async () => {
    try{
      console.log("?")
      const data = await login(email, password)
      console.log(data)
    }catch(e){
      console.log(e)
      return alert("エラーが発生しました。")
    }
  }


  return (
    <CustomLayout>
      <div className="login-box">
        <h3 className="flex jc">ログイン</h3>
        <Link className="signup-link" href={"/signup"}>
          アカウントをお持ちでない方はこちら
        </Link>
        <div className="mt-1">メール</div>
        <CustomInput
          placeholder="IDを入力してください。"
          classname="mt-05"
          text={email}
          setText={setEmail}
        />
        <div className="mt-1">パスワード</div>
        <CustomInput
          placeholder="パスワードを入力してください。"
          classname="mt-05"
          text={password}
          setText={setPassword}
        />
        <CustomButton func={onLogin} text="ログイン" classname="mt-2" />
      </div>
    </CustomLayout>
  );
}
