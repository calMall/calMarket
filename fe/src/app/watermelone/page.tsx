"use client";
import CustomLayout from "@/components/common/CustomLayout";
import ModalCover from "@/components/common/ModalCover";
import MatterGame from "@/components/game/suika"; // 경로는 실제 파일 위치에 맞게
import Link from "next/link";

export default function Page() {
  return (
    <CustomLayout>
      <ModalCover>
        <div className="wf hf">
          <Link className="ab flex ac jc watermelon-btn" href={"/"}>
            ホームに戻る
          </Link>
          <h2 className="flex ac jc">スイカゲーム</h2>
          <MatterGame />
        </div>
      </ModalCover>
    </CustomLayout>
  );
}
