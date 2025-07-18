"use client";

import Image from "next/image";
import Link from "next/link";
import { useState } from "react";
import { FaSearch } from "react-icons/fa";

export default function Header() {
  const [searchText, setSearchText] = useState("");
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

      <div className="flex jc ac wf rt search-bar mn">
        <input
          type="text"
          className="hf wf bx"
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
        />
        <button className="ab search-btn main-dark-btn-hover">
          <FaSearch />
        </button>
      </div>

      <div className="nowrap">
        <Link href={"/"}>ログイン</Link>
      </div>
    </div>
  );
}
