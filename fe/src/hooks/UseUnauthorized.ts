"use client";

import UserStore from "@/store/user";
import { useRouter } from "next/navigation";

export default function UseUnauthorized() {
  const userStore = UserStore();
  const router = useRouter();

  return () => {
    alert("ログインが必要です。ログインページに移動します。");
    userStore.logout();
    router.push("/login");
  };
}
