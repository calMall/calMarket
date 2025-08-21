"use client";

import UserStore from "@/store/user";
import { useEffect, useRef, useState } from "react";
import { IoPersonSharp } from "react-icons/io5";
import { MdLogout, MdOutlineArrowDropDown } from "react-icons/md";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { FaCartShopping } from "react-icons/fa6";
import { logout } from "@/api/User";

interface props {
  nickname: string;
}

export default function HeaderMenu({ nickname }: props) {
  const [viewMenu, setViewMenu] = useState(false);
  const onViewMenu = () => {
    setViewMenu(false);
  };

  const userStore = UserStore();
  const router = useRouter();
  const onLogout = async () => {
    try {
      setViewMenu(false);
      const data = await logout();
      console.log(data);
      if (data.message === "success") {
        userStore.logout();
        router.replace("/");
      }
    } catch (e) {
      alert("エラーが発生しました。");
    }
  };
  const ref = useRef<HTMLDivElement>(null);
  const parentRef = useRef<HTMLDivElement>(null);
  const toggleMenu = () => {
    setViewMenu((prev) => !prev);
  };
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (
        ref.current &&
        parentRef.current &&
        !parentRef.current.contains(event.target as Node) &&
        !ref.current.contains(event.target as Node)
      ) {
        setViewMenu(false);
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [ref, setViewMenu]);
  return (
    <div className="rt hf ac flex" ref={parentRef}>
      <button onClick={toggleMenu} className="fs-large flex ac jc">
        {nickname}
        <MdOutlineArrowDropDown />
      </button>
      {viewMenu && (
        <div className="content-menu-contain ab z-20" ref={ref}>
          <Link href={`/mypage`}>
            <button
              className="flex ac gap-05 menu-mypage-btn button-hover-color "
              onClick={onViewMenu}
            >
              <IoPersonSharp />
              マイページ
            </button>
          </Link>
          <Link href={`/cart`}>
            <button
              className="flex ac gap-05 menu-mypage-btn button-hover-color"
              onClick={onViewMenu}
            >
              <FaCartShopping />
              カート
            </button>
          </Link>
          <button
            onClick={onLogout}
            className="flex ac gap-05 pointer menu-logout-btn button-hover-color"
          >
            <MdLogout />
            ログアウト
          </button>
        </div>
      )}
    </div>
  );
}
