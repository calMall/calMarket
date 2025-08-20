"use client";

import UserStore from "@/store/user";
import Image from "next/image";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { FaSearch } from "react-icons/fa";
import HeaderMenu from "./HeaderMenu";

export default function Header() {
  const searchParams = useSearchParams();
  const q = searchParams.get("q");
  const [searchText, setSearchText] = useState("");
  const router = useRouter();
  const onSearch = () => {
    router.push(`/search?q=${searchText}`);
  };
  const userStore = UserStore();
  useEffect(() => {
    if (typeof q === "string") setSearchText(q);
  }, []);

  return (
    <div className="header flex ac jb gap-1">
      <Link href={"/"} className="flex ac">
        <div className="logo-img rt ">
          <Image
            src={"/logo-calmar.png"}
            alt="キャルマ"
            style={{ objectFit: "contain" }}
            priority
            fill
            sizes="100%"
          />
        </div>
      </Link>

      <div className="flex jc ac wf rt search-bar">
        <input
          type="text"
          className="hf wf bx"
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          onKeyUp={(e) => e.key === "Enter" && onSearch()}
        />
        <button
          className="ab search-btn main-dark-btn-hover"
          onClick={onSearch}
        >
          <FaSearch />
        </button>
      </div>

      <div className="nowrap hf ac flex">
        {userStore.userInfo ? (
          <HeaderMenu nickname={userStore.userInfo.nickname} />
        ) : (
          <Link href={"/login"}>ログイン</Link>
        )}
      </div>
    </div>
  );
}
