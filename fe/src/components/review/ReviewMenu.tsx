"use client";

import UserStore from "@/store/user";
import { useEffect, useRef, useState } from "react";
import { MdDeleteOutline, MdLogout } from "react-icons/md";
import { useRouter } from "next/navigation";
import { BsThreeDotsVertical } from "react-icons/bs";
import { RiEdit2Line } from "react-icons/ri";
import { deleteReview } from "@/api/Review";
import UseUnauthorized from "@/hooks/UseUnauthorized";
import Link from "next/link";

interface props {
  reviewId: number;
  refreshData: Function;
  itemCode: string;
}

export default function ReviewMenu({ reviewId, refreshData, itemCode }: props) {
  const [viewMenu, setViewMenu] = useState(false);
  const userStore = UserStore();
  const router = useRouter();
  const useUnauthorized = UseUnauthorized();
  const ref = useRef<HTMLDivElement>(null);
  const parentRef = useRef<HTMLDivElement>(null);
  const toggleMenu = () => {
    setViewMenu((prev) => !prev);
  };

  const onDelete = async () => {
    if (window.confirm("レビューを削除しますか？")) {
      refreshData(reviewId);
      try {
        const res = await deleteReview(reviewId);
        if (res.message === "success") {
          alert("レビューを削除しました。");
        }
      } catch (e: any) {
        if (e.status === 401) {
          return useUnauthorized();
        }
        return alert("レビューのいいね中にエラーが発生しました。");
      }
    }
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
    <div className="rt hf ac flex mr-1 mt-1" ref={parentRef}>
      <button onClick={toggleMenu} className="fs-large flex ac jc">
        <BsThreeDotsVertical />
      </button>
      {viewMenu && (
        <div className="content-menu-contain ab z-20" ref={ref}>
          <Link
            href={`/review/write/${itemCode}?reviewId=${reviewId}`}
            className="flex ac gap-05 pointer menu-mypage-btn button-hover-color"
          >
            <RiEdit2Line />
            編集
          </Link>
          <button
            onClick={onDelete}
            className="flex ac gap-05 pointer menu-logout-btn button-hover-color"
          >
            <MdDeleteOutline />
            削除
          </button>
        </div>
      )}
    </div>
  );
}
