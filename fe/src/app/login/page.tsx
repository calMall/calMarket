"use client";

import CustomButton from "@/components/common/CustomBtn";
import CustomInput from "@/components/common/CustomInput";
import CustomLayout from "@/components/common/CustomLayout";
import Link from "next/link";
import { useState } from "react";

export default function Login() {
  const [id, setId] = useState("");
  const [password, setPassword] = useState("");
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
          text={id}
          setText={setId}
        />
        <div className="mt-1">パスワード</div>
        <CustomInput
          placeholder="パスワードを入力してください。"
          classname="mt-05"
          text={password}
          setText={setPassword}
        />
        <CustomButton func={() => {}} text="ログイン" classname="mt-2" />
      </div>
    </CustomLayout>
  );
}
