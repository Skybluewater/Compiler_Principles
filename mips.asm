	.data
nums:	.word	8, 7, 2, 3, 10, 29, 21, 22, 6, 15, 22, 33, 45, 40, 100, 20, 33, 99, 0, 35
size:	.word	20
	.text
	
	addi $a0,$zero,0
	addi $a1,$zero,19
	jal quickSort
	jal print

# all register used: a0 a1 s0 stack:sp
quickSort:
	bge $a0,$a1,q_end # a0:low a1:high
	addi $sp,$sp,-12
	sw $ra,0($sp)
	sw $a0,4($sp)
	sw $a1,8($sp)
	jal partition # in: a0:low a1:high
	lw $s0,0($sp) # s0 is the new partition
	addi $sp,$sp,4 # add 4
	lw $a0,4($sp)
	addi $a1,$s0,-1
	jal quickSort
	lw $a1,8($sp)
	addi $a0,$s0,1
	jal quickSort
	lw $ra,0($sp)
	addi $sp,$sp,12
q_end:
	jr $ra

partition:
	addi $sp,$sp,-8
	sw $ra,0($sp)
	la $t0,nums
	sll $t1,$a1,2
	add $t2,$t1,$t0
	lw $s2,0($t2) # pivot
	addi $s0,$a0,-1 # i 
	move $s1,$a0 # j
forloop:
	sll $t1,$s1,2
	add $t2,$t0,$t1
	lw $t3,0($t2)
	bge $t3,$s2,elsefor
	addi $s0,$s0,1
	move $a2,$s0
	move $a3,$s1
	jal swap
elsefor:
	addi $s1,$s1,1
	blt $s1,$a1,forloop
	addi $s0,$s0,1
	move $a2,$s0
	move $a3,$a1
	jal swap
	lw $ra,0($sp)
	sw $s0,4($sp)
	addi $sp,$sp,4
	jr $ra
	
swap:
	.text 
	sll $a2,$a2,2
	sll $a3,$a3,2
	la $t0,nums
	add $t1,$a2,$t0
	add $t2,$a3,$t0
	lw $t3,0($t1)
	lw $t4,0($t2)
	sw $t4,0($t1)
	sw $t3,0($t2)
	jr $ra

	.data
space:	.asciiz " "
head:	.asciiz "The sorted numbers are:\n"
	.text
print:	la $t0, nums		#load address of number array
	lw $t1, size		#load integer of size of the array
	la $a0, head		#print header
	li $v0, 4
	syscall
output:	lw $a0, 0($t0)		#print value of nums array
	li $v0, 1
	syscall
	la $a0, space		#print space between numbers
	li $v0, 4
	syscall
	addi $t0, $t0, 4	#increment pointer inside nums array
	addi $t1, $t1, -1	#decrement loop counter
	bgtz $t1, output	#return back to start until loop counter is less than 0
exit:	li $v0, 10		#syscall to exit
	syscall