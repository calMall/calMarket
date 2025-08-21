"use client";

import UserStore from "@/store/user";
import { useEffect, useRef, useState } from "react";
import { MdDeleteOutline, MdLogout } from "react-icons/md";
import { useRouter } from "next/navigation";
import { BsThreeDotsVertical } from "react-icons/bs";
import { RiEdit2Line } from "react-icons/ri";

interface props {
  address: deliveryAddressDetail;
  onDelete: Function;
}

export default function DeliveryMenu({ address, onDelete }: props) {
  const [viewMenu, setViewMenu] = useState(false);
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
    <div className="rt hf ac flex mr-1 mt-1" ref={parentRef}>
      <button onClick={toggleMenu} className="fs-large flex ac jc">
        <BsThreeDotsVertical />
      </button>
      {viewMenu && (
        <div className="content-menu-contain ab z-20" ref={ref}>
          <button
            onClick={() => onDelete(address)}
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
