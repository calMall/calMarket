"use client";

import UserStore from "@/store/user";
import { useRouter } from "next/navigation";

export default function Unauthorized() {
  const userStore = UserStore();
  const router = useRouter();
  alert("ログインが必要です。ログインページに移動します。");
  userStore.logout();
  return router.push("/login");
}
